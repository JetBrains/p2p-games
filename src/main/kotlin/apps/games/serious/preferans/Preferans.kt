package apps.games.serious.preferans

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.primitives.Deck
import apps.games.primitives.EncryptedDeck
import apps.games.primitives.protocols.DeckShuffleGame
import apps.games.primitives.protocols.RandomDeckGame
import apps.games.serious.preferans.GUI.preferansGame
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import entity.ChatMessage
import entity.Group
import entity.User
import org.bouncycastle.jce.ECNamedCurveTable
import proto.GameMessageProto
import java.math.BigInteger
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue

/**
 * Created by user on 7/6/16.
 */

class preferans(chat: Chat, group: Group, gameID: String) : Game<Unit>(chat,
        group, gameID) {
    override val name: String
        get() = "preferans Card Game"

    private enum class State {
        INIT,
        ROUND_INIT,
        DECRYPT_HAND,
        BIDDING,
        VERIFY_BET,
        REVEAL_TALON,
        END
    }

    private val ECParams = ECNamedCurveTable.getParameterSpec("secp256k1")
    private var state: State = State.INIT

    private lateinit var gameGUI: preferansGame
    private lateinit var application: LwjglApplication

    private val DECK_SIZE = 32
    //TALON - always last two cards of the deck
    private val TALON = 2

    //to sorted array to preserve order
    private val playerOrder: List<User> = group.users.sortedBy { x -> x.name }
    private val playerID = playerOrder.indexOf(chat.me())

    //Required - three players.
    //TODO - add checker for number of players

    private val N = 3
    private val cardHolders: MutableMap<Int, Int> = mutableMapOf()

    //player whose turn is right now
    private var currentPlayer: Int = 0
    private val bets = Array(N, { i -> Bet.UNKNOWN })

    //Bet, that will be played this round
    private var gameBet: Bet = Bet.UNKNOWN

    //shuffled Deck
    private lateinit var deck: ShuffledDeck

    //Receive bets
    private val betQueue = LinkedBlockingQueue<Bet>(1)

    override fun getInitialMessage(): String {
        return playerOrder.hashCode().toString()
    }

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        when (state) {
            State.INIT -> {
                state = State.ROUND_INIT
                return initGame(responses)
            }
            State.ROUND_INIT -> {
                state = State.DECRYPT_HAND
                return initRound(responses)
            }
            State.DECRYPT_HAND -> {
                state = State.BIDDING
                return decryptHand(responses)
            }
            State.BIDDING -> {
                betQueue.clear()
                for (msg in responses) {
                    // TODO - digital signatures on player or encrypt channel
                    val userID = getUserID(User(msg.user))
                    if (userID == currentPlayer) {
                        bets[userID] = Bet.values().first { x -> x.value == msg.value.toInt() }
                    }
                }
                currentPlayer = (currentPlayer + 1) % N
                if (currentBet() != Bet.UNKNOWN) {
                    gameBet = currentBet()
                    state = State.VERIFY_BET
                    return gameBet.type
                }
                return getBid()
            }
            State.VERIFY_BET -> {
                state = State.REVEAL_TALON
                gameGUI.hideBiddingOverlay()
                val hashes = responses.map { x -> x.value }
                if (hashes.distinct().size != 1) {
                    throw GameExecutionException("Couldn't agree on final bet")
                }
                chat.sendMessage("We will play at least ${gameBet.type}")
                val keys = mutableListOf<BigInteger>()
                for (i in TALON downTo 1) {
                    keys.add(deck.encrypted.keys[DECK_SIZE - i])
                }
                gameGUI.hideBiddingOverlay()
                return keys.joinToString(" ")
            }
            State.REVEAL_TALON -> {
                state = State.END
                return decryptTalon(responses)
            }
            State.END -> {
            }
        }
        return ""
    }

    /**
     * Start GUI for the preferans game
     */
    private fun initGame(responses: List<GameMessageProto.GameStateMessage>): String {
        //validate player order
        val hashes = responses.distinctBy { x -> x.value }
        if (hashes.size != 1) {
            throw GameExecutionException("Someone has different deck")
        }

        val config = LwjglApplicationConfiguration()
        config.width = 1024
        config.height = 1024
        config.forceExit = false
        config.title = "preferans Game[${chat.username}]"
        gameGUI = preferansGame()
        application = LwjglApplication(gameGUI, config)
        while (!gameGUI.loaded) {
            Thread.sleep(200)
        }
        return ""
    }

    /**
     * Start next round of the game:
     * create and shuffle deck. compute who holds which card
     * return keys for cards that I don't hold
     */
    private fun initRound(responses: List<GameMessageProto.GameStateMessage>): String {
        gameGUI.showHint("Shuffling cards")
        //If we can not create deck - game aborted
        deck = newDeck() ?: return ""
        //Deal all cards, except last two
        cardHolders.clear()
        val resultKeys = mutableListOf<BigInteger>()

        for (i in 0..deck.originalDeck.size - 1 - TALON) {
            val holder = i % N
            cardHolders[i] = holder
            if (holder != playerID) {
                resultKeys.add(deck.encrypted.keys[i])
            }
        }
        return resultKeys.joinToString(" ")
    }

    /**
     * Given responses from ROUND_INIT stage
     * decrypt cards in my hand
     */
    private fun decryptHand(responses: List<GameMessageProto.GameStateMessage>): String {
        deck.encrypted.deck.decryptSeparate(deck.encrypted.keys)
        for (msg in responses) {
            val keys = msg.value.split(" ").map { x -> BigInteger(x) }
            decryptWithUserKeys(User(msg.user), keys)
        }
        dealHands()
        gameGUI.showBiddingOverlay()
        gameGUI.disableAllBets()
        registerCallbacks()
        currentPlayer = -1
        return ""
    }


    /**
     * Show bidding overlay, get bet from
     * the player
     */
    private fun getBid(): String {
        val toDisplay = Array(N, { i -> Pair(playerOrder[i], bets[i]) })
        //display bets of players, also disables them
        if (playerID == currentPlayer && bets[playerID] != Bet.PASS) {
            gameGUI.resetAllBets()
            gameGUI.markBets(*toDisplay)
            //you can choose what chose last time
            gameGUI.disableBets(
                    *(bets.filter { x -> x != Bet.PASS && x != bets[playerID] }.toTypedArray()))
            val maxBet = bets.maxBy { x -> x.value } ?: throw GameExecutionException(
                    "Womething went wront in betting")
            gameGUI.disableAllBets()
            //player alvays can bet higher then current bidding
            gameGUI.enableBets(
                    *Bet.values().filter { x -> x.value > maxBet.value }.toTypedArray())

            //If player is currently highest bidder - he can stick to the same bet
            //if he is not - he can pass
            if (bets[playerID] == maxBet) {
                gameGUI.enableBets(bets[playerID])
                gameGUI.showHint(
                        "Your turn! You can bid [${maxBet.type}] or higher")
            } else {
                gameGUI.enableBets(Bet.PASS)
                gameGUI.showHint(
                        "Your turn! You can bid [${maxBet.type}] or higher or PASS")
            }
            val bet = betQueue.take()
            gameGUI.disableAllBets()
            return bet.value.toString()
        } else {
            gameGUI.resetAllBets()
            gameGUI.disableAllBets()
            gameGUI.markBets(*toDisplay)
            gameGUI.showHint(
                    "Waiting for [${playerOrder[currentPlayer].name}] to make his move")
            if (bets[playerID] == Bet.PASS) {
                return Bet.PASS.value.toString()
            }
            return ""
        }
    }

    /**
     * Receive keys of talon, decrypt it, show to everyone
     */
    fun decryptTalon(responses: List<GameMessageProto.GameStateMessage>): String {
        gameGUI.showHint("Revealing talon")
        for (msg in responses) {
            if (User(msg.user) == chat.me()) {
                continue
            }
            val keys = msg.value.split(" ").map { x -> BigInteger(x) }
            if (keys.size != TALON) {
                throw GameExecutionException(
                        "Someone sent incorrect number of talon keys")
            }
            for (i in 0..TALON - 1) {
                deck.encrypted.deck.decryptCardWithKey(DECK_SIZE - TALON + i,
                        keys[i])
            }
        }
        for (i in 0..TALON - 1) {
            val index = deck.originalDeck.cards.indexOf(
                    deck.encrypted.deck.cards[DECK_SIZE - TALON + i])
            if (index == -1) {
                throw GameExecutionException(
                        "Someone sent incorrect talon keys")
            }
            //reveal Common
            gameGUI.revealTalonCard(index)
        }
        return ""
    }


    /**
     * Create a new deck and shuffle it.
     * In preferans this is executed before
     * each round
     * @return Pair of original Deck and
     * shuffle result - EncryptedDeck
     */
    private fun newDeck(): ShuffledDeck? {
        val deckFuture = runSubGame(
                RandomDeckGame(chat, group.clone(), subGameID(), ECParams,
                        DECK_SIZE))
        val deck: Deck
        try {
            deck = deckFuture.get()
        } catch(e: CancellationException) { // Task was cancelled - means that we need to stop. NOW!
            state = State.END
            return null
        } catch(e: ExecutionException) {
            chat.showMessage(
                    ChatMessage(chat, e.message ?: "Something went wrong"))
            e.printStackTrace()
            throw GameExecutionException("Subgame failed")
        }

        val shuffleFuture = runSubGame(
                DeckShuffleGame(chat, group.clone(), subGameID(), ECParams,
                        deck.clone()))
        val shuffled: EncryptedDeck
        try {
            shuffled = shuffleFuture.get()
        } catch(e: CancellationException) { // Task was cancelled - means that we need to stop. NOW!
            state = State.END
            return null
        } catch(e: ExecutionException) {
            chat.showMessage(
                    ChatMessage(chat, e.message ?: "Something went wrong"))
            e.printStackTrace()
            throw GameExecutionException("Subgame failed")
        }
        return ShuffledDeck(deck, shuffled)
    }

    /**
     * Take a list of keys sent by user,
     * keys should correspond to cards, that are not
     * by that user. I.E. If player holds cards
     * 1, 4, 7 in shuffled deck, keys - contains
     * key for every card, that is not in TALON,
     * and are not possesed by that user
     */
    private fun decryptWithUserKeys(user: User, keys: List<BigInteger>) {
        val positions = mutableListOf<Int>()
        for (key in cardHolders.keys) {
            if (cardHolders[key] != getUserID(user)) {
                positions.add(key)
            }
        }
        if (positions.size != keys.size) {
            throw GameExecutionException(
                    "Someone failed to provide correct keys for encrypted cards")
        }
        for (i in 0..positions.size - 1) {
            val position = positions[i]
            deck.encrypted.deck.decryptCardWithKey(position, keys[i])
        }
    }

    /**
     * Give each player cards, that
     * belong to his hand(GUI)
     */
    private fun dealHands() {
        gameGUI.showHint("Dealing hands")
        gameGUI.tableScreen.showDeck()
        for (i in 0..DECK_SIZE - TALON - 1) {
            val cardID: Int
            if (cardHolders[i] == playerID) {
                cardID = deck.originalDeck.cards.indexOf(
                        deck.encrypted.deck.cards[i])
                if (cardID == -1) {
                    throw GameExecutionException(
                            "I can not decrypt my own cards")
                }
            } else {
                cardID = -1
            }
            var currentPlayerId: Int = cardHolders[i] ?: throw GameExecutionException(
                    "Invalid card distribution")
            currentPlayerId -= playerID
            if (currentPlayerId < 0) {
                currentPlayerId += N
            }
            gameGUI.dealPlayer(currentPlayerId, cardID)
        }
        //Deal unknown Talon cards
        for (i in 1..TALON) {
            gameGUI.dealCommon(-1)
        }
        gameGUI.tableScreen.hideDeck()
    }

    /**
     * Get current bet result:
     * UNKNOWN - if agreement is not met yet
     * or round is not finished
     *
     * Otherwise - return Bet that is played
     */
    private fun currentBet(): Bet {
        if (currentPlayer != 0 || bets.contains(Bet.UNKNOWN)) {
            return Bet.UNKNOWN
        }
        when (bets.count { x -> x == Bet.PASS }) {
            N -> return Bet.PASS
            N - 1 -> return bets.first { x -> x != Bet.PASS }
            else -> return Bet.UNKNOWN
        }
    }

    fun registerCallbacks() {
        val callback = { x: Bet ->
            betQueue.offer(x)
        }
        gameGUI.registerCallback(callback, *Bet.values())
    }

    fun getUserID(user: User): Int {
        return playerOrder.indexOf(user)
    }

    override fun getResult() {
        return Unit
    }
}

data class ShuffledDeck(val originalDeck: Deck, val encrypted: EncryptedDeck)