package apps.games.serious.mafia.subgames.sum

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.GameManager
import apps.games.GameManagerClass
import crypto.RSA.RSAKeyManager
import crypto.random.randomString
import entity.Group
import entity.User
import org.bouncycastle.crypto.InvalidCipherTextException
import proto.GameMessageProto
import java.math.BigInteger

/**
 * Created by user on 8/25/16.
 */


class SecureMultipartySumGame(chat: Chat, group: Group, gameID: String, val keyManager: RSAKeyManager,
                              val n: BigInteger, gameManager: GameManagerClass = GameManager) : Game<Pair<BigInteger, SMSVerifier>>(chat, group, gameID, gameManager = gameManager) {
    override val name: String
        get() = "First part of SMS algorithm"

    val playerOrder = group.users.sortedBy { x -> x.name }.toMutableList()
    val playerID = playerOrder.indexOf(chat.me())
    var currentPlayer: Int = -1
    val N = group.users.size
    private val SALT_LENGTH = 128

    private enum class State {
        INIT,
        SPLIT,
        SUM,
        END
    }

    private var state: State = State.INIT
    private var sum: BigInteger = BigInteger.ZERO
    private val verifier = SMSVerifier()
    private val parts = crypto.random.split(n, N)

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        for (msg in responses) {
            chat.showMessage(msg.value)
        }
        when (state) {
            State.INIT -> {
                for (msg in responses) {
                    keyManager.registerUserPublicKey(User(msg.user), msg.value)
                }
                currentPlayer = -1
                state = State.SPLIT
            }
            State.SPLIT -> {
                for (msg in responses) {
                    if (currentPlayer >= 0 && currentPlayer < N) {
                        verifier.registerMessage(User(msg.user), playerOrder[currentPlayer], msg.value)
                    }
                    if (playerID == currentPlayer) {
                        try {
                            val split = keyManager.decodeString(msg.value).split(" ")
                            sum += BigInteger(split[1])
                        } catch (e: InvalidCipherTextException) {
                            throw GameExecutionException("Malformed RSA key")
                        }
                    }
                }
                currentPlayer++
                if (currentPlayer < N) {
                    val msg = randomString(SALT_LENGTH) + " " + parts[currentPlayer].toString()
                    return keyManager.encodeForUser(playerOrder[currentPlayer], msg)
                } else {
                    state = State.SUM
                    return sum.toString()
                }

            }
            State.SUM -> {
                verifier.registerPartialSum(chat.me(), sum)
                for (msg in responses) {
                    val userID = playerOrder.indexOf(User(msg.user))
                    if (userID != playerID) {
                        val pSum = BigInteger(msg.value)
                        sum += pSum
                        verifier.registerPartialSum(User(msg.user), pSum)
                    }

                }
                verifier.registerFinalSum(sum)
                state = State.END
                return ""
            }
            State.END -> {
            }
        }
        return ""
    }

    override fun getInitialMessage(): String {
        return keyManager.getPublicKey()
    }

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun getResult(): Pair<BigInteger, SMSVerifier> {
        return sum to verifier
    }

}