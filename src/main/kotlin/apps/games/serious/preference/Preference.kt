package apps.games.serious.preference

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.primitives.Deck
import apps.games.primitives.EncryptedDeck
import apps.games.primitives.protocols.DeckShuffleGame
import apps.games.primitives.protocols.RandomDeckGame
import entity.ChatMessage
import entity.Group
import org.bouncycastle.jce.ECNamedCurveTable
import proto.GameMessageProto
import java.math.BigInteger
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

/**
 * Created by user on 7/6/16.
 */

class Preference(chat: Chat, group: Group, gameID: String) : Game<Unit>(chat, group, gameID){
    override val name: String
        get() = "Preference Card Game"

    private enum class State{
        INIT,
        ROUND_INIT,
        END
    }

    private val ECParams = ECNamedCurveTable.getParameterSpec("secp256k1")
    private var state: State = State.INIT

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        when(state){
            State.INIT -> {
                state = State.END
            }
            State.END -> {}
        }
        return ""
    }

    override fun getResult() {
        return Unit
    }

    /**
     * Create a new deck and shuffle it.
     * In preference this is executed before
     * each round
     * @return Pair of original Deck and
     * shuffle result - EncryptedDeck
     */
    private fun newDeck(): Pair<Deck, EncryptedDeck>?{
        val deckFuture = runSubGame(RandomDeckGame(chat, group.clone(), subGameID(), ECParams, 3))
        val deck: Deck
        try{
            deck = deckFuture.get()
        }catch(e: CancellationException){ // Task was cancelled - means that we need to stop. NOW!
            state = State.END
            return null
        }catch(e: ExecutionException){
            chat.showMessage(ChatMessage(chat, e.message?: "Something went wrong"))
            throw GameExecutionException("Subgame failed")
        }

        val shuffleFuture = runSubGame(DeckShuffleGame(chat, group.clone(), subGameID(), ECParams, deck))
        val shuffled: EncryptedDeck
        try{
            shuffled = shuffleFuture.get()
        }catch(e: CancellationException){ // Task was cancelled - means that we need to stop. NOW!
            state = State.END
            return null
        }catch(e: ExecutionException){
            chat.showMessage(ChatMessage(chat, e.message?: "Something went wrong"))
            throw GameExecutionException("Subgame failed")
        }
        return Pair(deck, shuffled)
    }

}
