package apps.games.serious.mafia.subgames.sum

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.GameManager
import apps.games.GameManagerClass
import crypto.RSA.ECParams
import crypto.RSA.RSAKeyManager
import crypto.random.randomBigInt
import crypto.random.randomString
import entity.Group
import entity.User
import org.apache.commons.codec.digest.DigestUtils
import proto.GameMessageProto
import java.math.BigInteger
import java.util.concurrent.CancellationException

/**
 * Created by Wimag on 8/28/16.
 *
 * @param n - our input value
 * @param MAXN - max value for input over all players
 */
class SecureMultipartySumForAnonymizationGame(chat: Chat, group: Group, gameID: String, val keyManager: RSAKeyManager,
                              val n: BigInteger, val MAXN: BigInteger = ECParams.n, gameManager: GameManagerClass = GameManager) : Game<SMSfAResult>(chat, group, gameID, gameManager = gameManager) {
    override val name: String
        get() = "Secure multiparty sum for anonymization"

    private enum class State{
        INIT,
        VERIFY_HASHES,
        END
    }

    private var state: State = State.INIT
    private val R = randomBigInt(MAXN)
    private val SALT_LENGTH = 128
    private val SALT = randomString(SALT_LENGTH)
    private val RHashes = mutableMapOf<User, String>()
    private lateinit var verifier: SMSVerifier
    private lateinit var totalSum: BigInteger

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        for(msg in responses){
            chat.showMessage(msg.value)
        }
        when(state){
            State.INIT -> {
                state = State.VERIFY_HASHES
                return DigestUtils.sha256Hex(SALT + " " + R.toString())
            }
            State.VERIFY_HASHES -> {
                for(msg in responses){
                    RHashes[User(msg.user)] = msg.value
                }
                val SMSGame = SecureMultipartySumGame(chat, group, subGameID(), keyManager, R + n, gameManager)
                try {
                    val sumFuture = gameManager.initSubGame(SMSGame)
                    totalSum = sumFuture.get().first
                    verifier = sumFuture.get().second
                } catch (e: CancellationException){
                    state = State.END
                    return ""
                } catch (e: Exception){
                    throw GameExecutionException(e.message?: "Something wrong in SMS")
                }
                state = State.END
            }
            State.END -> {}
        }
        return ""
    }

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun getResult(): SMSfAResult {
        return SMSfAResult(totalSum, SALT, R, RHashes, verifier)
    }
}

data class SMSfAResult(val sum: BigInteger, val salt: String, val R: BigInteger, val RHashes: Map<User, String>, val SMSVerifier: SMSVerifier)