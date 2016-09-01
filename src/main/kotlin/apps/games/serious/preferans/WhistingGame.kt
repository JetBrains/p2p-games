package apps.games.serious.preferans

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.GameManagerClass
import apps.games.serious.preferans.GUI.PreferansGame
import entity.Group
import entity.User
import proto.GameMessageProto
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by user on 7/13/16.
 */
private val realWhists = listOf(Whists.WHIST_BLIND, Whists.WHIST_OPEN)

class WhistingGame(chat: Chat, group: Group, gameID: String, gameManager:
GameManagerClass, val gameGUI: PreferansGame,
                   val maxBet: Bet) : Game<Whists>(chat, group, gameID, gameManager) {
    override val name: String
        get() = "Whisting game"

    private enum class State {
        INIT,
        BID,
        END
    }

    private enum class Round {
        BID,
        REBID,
        OPEN_CARDS
    }

    private var state: State = State.INIT
    private val playerOrder: List<User> = group.users.sortedBy { x -> x.name }
    private val playerID = playerOrder.indexOf(chat.me())
    private var currentPlayer = -1
    private val N = 2
    private val whists = Array(N, { i -> Whists.UNKNOWN })
    private val whistQueue = LinkedBlockingQueue<Whists>(1)
    private var result: Whists = Whists.UNKNOWN
    // round of negotiation
    private var round: Round = Round.BID
    // union of Blind Whist and Normal Whist
    //TODO - show only one button instead of two

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        //log everything to all-chat
        for (msg in responses) {
            chat.sendMessage(msg.value)
        }
        when (state) {
            State.INIT -> {
                if (N != 2) {
                    throw GameExecutionException("In 3-man Preferans whisting" +
                            " goes between 2 of " +
                            "them")
                }
                state = State.BID
                gameGUI.showWhistingOverlay()
                registerCallbacks()
            }
            State.BID -> {
                for (msg in responses) {
                    val userId = getUserID(User(msg.user))
                    if (userId == currentPlayer) {
                        whists[userId] = Whists.valueOf(msg.value)
                    }
                }
                currentPlayer++

                val toDisplay = Array(N, { i -> Pair(playerOrder[i], whists[i]) })
                gameGUI.resetAllWhists()
                gameGUI.markWhists(*toDisplay)

                translateState()
                if (state == State.END) {
                    gameGUI.hideWhistingOverlay()
                    return whists[playerID].name
                }
                gameGUI.disableAllWhists()
                if (playerID == currentPlayer) {
                    //If this is the first bet in series
                    when (round) {
                        Round.BID -> {
                            showBidRoundWhists()
                        }

                        Round.REBID -> {
                            showRebidRoundWhists()
                            //if first playerId whisted - auto pass
                            //if he didn't - keep our half whist
                            if (playerID == 1) {
                                if (whists[0] in realWhists) {
                                    return Whists.PASS.name
                                } else {
                                    return Whists.WHIST_HALF.name
                                }
                            }
                        }
                        Round.OPEN_CARDS -> {
                            if (whists[playerID] == Whists.PASS) {
                                return Whists.PASS.name
                            }
                            showOpenCardRoundWhists()
                        }
                    }
                    whistQueue.clear()
                    val whist = whistQueue.take()
                    gameGUI.resetAllWhists()
                    gameGUI.markWhists(*toDisplay)
                    return whist.name

                } else {
                    gameGUI.showHint("[${maxBet.type}] Waiting for other playerId to" +
                            " " +
                            "decide " +
                            "on " +
                            "whisting")
                }
            }
            State.END -> TODO()
        }
        return ""
    }

    /**
     * check if round of negotionation is over.
     * It it is over - perform transition to next
     * round according to rules of preferans
     */
    private fun translateState() {
        if (currentPlayer == N) {
            currentPlayer = 0

            when (round) {
                Round.BID -> {
                    //PASS -> PASS = PASS
                    if (whists[0] == Whists.PASS && whists[1] == Whists.PASS) {
                        state = State.END
                    }
                    //WHIST -> WHIST = WHIST_BLIND
                    if (whists[0] in realWhists && whists[1] in realWhists) {
                        whists[playerID] = Whists.WHIST_BLIND
                        state = State.END
                    }
                    //PASS -> HALF WHIST -> REBID
                    if (whists[1] == Whists.WHIST_HALF) {
                        round = Round.REBID
                    }
                    //PASS -> WHIST or WHIST -> PASS -> decide if
                    // open cards
                    if (whists.contains(Whists.PASS) && whists
                            .intersect(realWhists).isNotEmpty()) {
                        round = Round.OPEN_CARDS
                    }
                }
                Round.REBID -> {
                    //PASS -> HALF WHIST -> REBID(nothing changed)
                    if (whists[1] == Whists.WHIST_HALF) {
                        state = State.END
                    }
                    //WHIST -> PASS
                    if (whists[0] in realWhists) {
                        round = Round.OPEN_CARDS
                    }
                }
                Round.OPEN_CARDS -> {
                    state = State.END
                }
            }
        }
    }

    /**
     * if it is first Whist bidding round -
     * everyone can pass or whist.
     */
    fun showBidRoundWhists() {

        //Everyone can pass or whist in first round
        gameGUI.showHint("[${maxBet.type}] You can PASS or WHIST(Both " +
                "whist are equal)")
        gameGUI.enableWhists(Whists.PASS, Whists.WHIST_BLIND,
                Whists.WHIST_OPEN)
        //if first playerId passed - second can go half whist
        if (playerID == 1 && whists[0] == Whists.PASS && maxBet.value <= Bet.MIZER.value) {
            gameGUI.showHint("[${maxBet.type}] You can PASS or WHIST or " +
                    "HALF WHIST(Both " +
                    "whist are equal)")
            gameGUI.enableWhists(Whists.WHIST_HALF)
        }
    }

    /**
     * If second playerId Half-Whisted - firs playerId can upgrade his
     * pass to whist
     */
    fun showRebidRoundWhists() {
        //first playerId can pass or whist
        if (playerID == 0) {
            gameGUI.showHint("[${maxBet.type}] You can PASS or WHIST(Both " +
                    "whist are equal)")
            gameGUI.enableWhists(Whists.PASS,
                    Whists.WHIST_OPEN,
                    Whists.WHIST_BLIND)
        }
    }

    /**
     * If one whisted and second didn't - first
     * can choose whether to disclose his cards,
     * or not
     */
    fun showOpenCardRoundWhists() {
        if (whists[playerID] in realWhists) {
            gameGUI.showHint("[${maxBet.type}] You can choose what WHIST " +
                    "to play")
            gameGUI.enableWhists(*realWhists.toTypedArray())
        }
    }

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun getResult(): Whists {
        return whists[playerID]
    }

    fun registerCallbacks() {
        val callback = { x: Whists ->
            whistQueue.offer(x)
        }
        gameGUI.registerWhistingCallback(callback, *Whists.values())
    }

    fun getUserID(user: User): Int {
        return playerOrder.indexOf(user)
    }

    companion object {
        /**
         * check if given array of whists coulb be aquired from this game
         * @param whists - array of whisting outcomes in the same order as
         * whisted
         * @return highest whist - unknown if something is wrong
         */
        fun verifyWhists(whists: Array<Whists>): Whists {
            var possible: Boolean = true
            if (whists.size != 2) {
                possible = false
            }
            possible = possible &&
                    ((whists[0] == Whists.PASS && whists[1] in realWhists) ||
                            (whists[0] == Whists.PASS && whists[1] == Whists.PASS) ||
                            (whists[0] == Whists.PASS && whists[1] == Whists.WHIST_HALF)) ||
                    (whists[0] in realWhists && whists[1] == Whists.PASS) ||
                    (whists[0] == Whists.WHIST_BLIND && whists[1] == Whists.WHIST_BLIND)
            if (!possible) {
                return Whists.UNKNOWN
            }
            return whists.maxBy { x -> x.value } ?: return Whists.UNKNOWN
        }
    }
}