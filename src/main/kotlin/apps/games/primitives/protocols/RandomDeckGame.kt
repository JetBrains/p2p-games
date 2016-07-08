package apps.games.primitives.protocols

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.GameManager
import apps.games.GameManagerClass
import apps.games.primitives.Deck
import apps.games.serious.lotto.Lotto
import com.sun.xml.internal.fastinfoset.util.StringArray
import crypto.random.randomECPoint
import crypto.random.randomString
import entity.ChatMessage
import entity.Group
import org.apache.commons.codec.digest.DigestUtils
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.math.ec.ECCurve
import org.bouncycastle.math.ec.ECPoint
import org.mockito.internal.matchers.Null
import proto.GameMessageProto
import java.math.BigInteger
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future

/**
 * Created by user on 7/5/16.
 *
 * Class describes protocol for generating common
 * random deck of given number of cards(Size)
 *
 * Algorithm has three stages
 *
 * INIT - all players agree on deck(generate (Size) common
 * random numbers using RandomNumberGame
 *
 * VALIDATE - compare deck hashes to ensure everyone has
 * the same deck
 *
 * END - end of the protocol
 */


class RandomDeckGame(chat: Chat, group: Group, gameID: String, val ECParams: ECParameterSpec,
                     val deckSize: Int = 52, gameManager: GameManagerClass = GameManager) : Game<Deck>(chat, group, gameID, gameManager = gameManager){
    override val name: String
        get() = "Random Deck Generator"

    private enum class State{
        INIT,
        VALIDATE,
        END
    }

    private var state: State = State.INIT

    override fun isFinished(): Boolean {
        return state == State.END
    }

    private val deck = Deck(ECParams, deckSize)

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        when(state){
            State.INIT -> {
                var set: Int = 0
                while(set < deckSize) {
                    val multiplier: BigInteger
                    val rngFuture = runSubGame(RandomNumberGame(chat, group.clone(), subGameID(), BigInteger.ONE, ECParams.n, gameManager))

                    try {
                        multiplier = rngFuture.get()
                    } catch(e: CancellationException) { // Task was cancelled - means that we need to stop. NOW!
                        state = State.END
                        return ""
                    } catch(e: ExecutionException) {
                        chat.showMessage(ChatMessage(chat, e.message ?: "Something went wrong"))
                        e.printStackTrace()
                        throw GameExecutionException("[${chat.me().name}] Subgame failed")
                    }
                    val point: ECPoint = ECParams.g.multiply(multiplier)
                    if (!deck.contains(point)) {
                        deck.cards[set] = point
                        set++
                    }

                }
                state = State.VALIDATE
                return deck.hashCode().toString()
            }
            State.VALIDATE -> {
                val hashes = responses.map { x -> x.value }
                if(hashes.distinct().size != 1){
                    throw GameExecutionException("Someone has a different deck")
                }
                state = State.END
            }
            State.END -> { }
        }
        return ""
    }

    override fun getFinalMessage(): String {
        return "Everything appears to be OK. My deck is: \n ${deck.toString()}"
    }

    override fun getResult(): Deck {
        return deck
    }
}
