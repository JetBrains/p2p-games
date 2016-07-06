package apps.games.serious

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.primitives.Deck
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
        END
    }

    private var state: State = State.INIT

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        when(state){
            State.INIT -> {
                val ECParams = ECNamedCurveTable.getParameterSpec("secp256k1")
                var deckFuture = runSubGame(RandomDeckGame(chat, group.clone(), subGameID(), ECParams, 3))
                val result: Deck
                try{
                    result = deckFuture.get()
                }catch(e: CancellationException){ // Task was cancelled - means that we need to stop. NOW!
                    state = State.END
                    return ""
                }catch(e: ExecutionException){
                    chat.showMessage(ChatMessage(chat, e.message?: "Something went wrong"))
                    throw GameExecutionException("Subgame failed")
                }
                val shuffleFuture = runSubGame(DeckShuffleGame(chat, group.clone(), subGameID(), ECParams, result))
                shuffleFuture.get()
                state = State.END
            }
            State.END -> {}
        }
        return ""
    }

    override fun getResult() {
        return Unit
    }

}
