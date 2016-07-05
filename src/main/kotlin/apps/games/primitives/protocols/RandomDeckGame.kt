package apps.games.primitives.protocols

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.primitives.Deck
import apps.games.serious.Lotto
import com.sun.xml.internal.fastinfoset.util.StringArray
import crypto.random.randomECPoint
import crypto.random.randomString
import entity.ChatMessage
import entity.Group
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.math.ec.ECPoint
import proto.GameMessageProto
import java.math.BigInteger
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

/**
 * Created by user on 7/5/16.
 */


class RandomDeckGame(chat: Chat, group: Group, gameID: String, val ECParams: ECParameterSpec, val deckSize: Int = 52) : Game(chat, group, gameID){
    override val name: String
        get() = "Random Deck Generator"

    private enum class State{
        INIT,
        END
    }

    private var state: State = State.INIT

    override fun isFinished(): Boolean {
        return state == State.END
    }

    private val cards = Deck(ECParams, deckSize)

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        when(state){
            State.INIT -> {
                var set: Int = 0
                while(set < deckSize) {
                    val rngFuture = runSubGame(RandomNumberGame(chat, group.clone(), subGameID(), BigInteger.ONE, ECParams.n))
                    val result: String
                    try {
                        result = rngFuture.get()
                    } catch(e: CancellationException) { // Task was cancelled - means that we need to stop. NOW!
                        state = State.END
                        return ""
                    } catch(e: ExecutionException) {
                        chat.showMessage(ChatMessage(chat, e.message ?: "Something went wrong"))
                        throw GameExecutionException("Subgame failed")
                    }
                    val multiplier: BigInteger
                    try {
                        multiplier = BigInteger(result)
                    } catch(e: Exception) {
                        throw GameExecutionException("Subgame returned unexpected result")
                    }
                    val point: ECPoint = ECParams.g.multiply(multiplier)
                    if(!cards.contains(point)){
                        cards.set(set, point)
                        set ++;
                    }
                }
                state = State.END
            }
            //TODO - maybe add validation. I.E - sort points, compute hash and send it
            State.END -> {
                return ""
            }
        }
        return ""
    }

    override fun getFinalMessage(): String {
        return "Everything appears to be OK. My deck is: \n ${cards.toString()}"
    }
}
