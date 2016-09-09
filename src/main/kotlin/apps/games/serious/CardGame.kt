package apps.games.serious

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.GameManager
import apps.games.GameManagerClass
import apps.games.primitives.Deck
import apps.games.primitives.EncryptedDeck
import apps.games.primitives.protocols.DeckShuffleGame
import apps.games.primitives.protocols.RandomDeckGame
import crypto.rsa.ECParams
import entity.ChatMessage
import entity.Group
import entity.User
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

/**
 * Created by user on 7/29/16.
 *
 * Base class for various utils used in many talbe games
 */

abstract class CardGame(chat: Chat, group: Group, gameID: String,
                        var deckSize: Int,
                        gameManager: GameManagerClass = GameManager
) : Game<Unit>(chat, group, gameID, gameManager) {
    //to sorted array to preserve order
    protected val playerOrder: MutableList<User> = group.users.sortedBy { x -> x.name }.toMutableList()
    protected var playerID = playerOrder.indexOf(chat.me())

    //shuffled Deck
    protected lateinit var deck: ShuffledDeck

    //playerId whose turn is right now
    protected var currentPlayerID: Int = 0

    /**
     * return nothing
     */
    override fun getResult() {
        return Unit
    }

    /**
     * get ID of user in playerOrder
     */
    fun getUserID(user: User): Int {
        return playerOrder.indexOf(user)
    }


    /**
     * During the game in gui - ew are always playerId 0,
     * meanwhile in game we are not
     */
    fun getTablePlayerId(id: Int): Int {
        var res: Int = id - playerID
        if (res < 0) {
            res += group.users.size
        }
        return res
    }

    /**
     * Create a new deck and shuffle it.
     * In Preferans this is executed before
     * each round
     * @return Pair of original Deck and
     * shuffle result - EncryptedDeck
     */
    private fun newDeck(): ShuffledDeck? {
        val deckFuture = runSubGame(
                RandomDeckGame(chat, group.clone(), subGameID(), ECParams, deckSize))
        val deck: Deck
        try {
            deck = deckFuture.get()
        } catch(e: CancellationException) {
            // Task was cancelled - means that we need to stop. NOW!
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
        } catch(e: CancellationException) {
            // Task was cancelled - means that we need to stop. NOW!
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
     * Genereate a new deck and set deck to ackording value
     */
    protected fun updateDeck(size: Int = deckSize) {
        deckSize = size
        deck = newDeck() ?: throw GameExecutionException("Couldn't generate deck")
    }


}