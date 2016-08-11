package apps.games.serious.mafia

import apps.chat.Chat
import apps.games.GameExecutionException
import apps.games.serious.CardGame
import apps.games.serious.Cheat.GUI.CheatGame
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import crypto.RSA.RSAKeyManager
import crypto.random.randomString
import entity.Group
import entity.User
import org.bouncycastle.crypto.InvalidCipherTextException
import proto.GameMessageProto


/**
 * Created by user on 8/9/16.
 */

class Mafia(chat: Chat, group: Group, gameID: String) : CardGame(chat, group, gameID, 36) {
    override val name: String
        get() = "mafia game"

    private enum class State{
        INIT,
        VALIDATE_KEYS,
        LOOP,
        END,
    }

    private lateinit var gameGUI: CheatGame
    private lateinit var application: LwjglApplication

    private var state: State = State.INIT
    private val keyManager = RSAKeyManager()
    private val HANDSHAKE_PHRASE = "HANDSHAKE" // who would've thought
    private val WINNING_PHRASE = "I DECLARE, WITH UTTER CERTAINTY, THAT THIS ONE IS IN THE BAG!"
    private val LOSING_PHRASE = "I LOST"

    private val N: Int
    private val M: Int
    init {
        N = group.users.size
        //if(N < 3) throw GameExecutionException("mafia is only viable with 4+ players")
        M = Math.sqrt(N * 1.0).toInt()
        //initGame()
    }



    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        for (msg in responses) {
            chat.showMessage("[${msg.user.name}] said ${msg.value}")
        }
        when(state){

            Mafia.State.INIT -> {
                for (msg in responses) {
                    keyManager.registerUserPublicKey(User(msg.user), msg.value)
                }
                currentPlayerID = -1
                state = State.VALIDATE_KEYS
            }
            Mafia.State.VALIDATE_KEYS -> {
                if (playerID == currentPlayerID) {
                    for (msg in responses) {
                        try {
                            val s = keyManager.decodeString(msg.value)
                            val handshake = s.split(" ").last()
                            if (handshake != HANDSHAKE_PHRASE) {
                                throw GameExecutionException("Invalid RSA key")
                            }
                        } catch (e: InvalidCipherTextException) {
                            throw GameExecutionException("Malformed RSA key")
                        }
                    }
                }

                currentPlayerID++
                if (currentPlayerID == N) {
                    state = State.END //TODO
                    chat.sendMessage("RSA is OK. Generating deck")
                    currentPlayerID = -1
                    return ""

                }
                val s = randomString(512) + " " + HANDSHAKE_PHRASE
                return keyManager.encodeForUser(playerOrder[currentPlayerID], s)
            }
            Mafia.State.LOOP -> {}
            Mafia.State.END -> TODO()
        }
        return ""
    }

    /**
     * Start GUI for the Cheat game
     */
    private fun initGame(): String {
        val config = LwjglApplicationConfiguration()
        config.width = 1024
        config.height = 1024
        config.forceExit = false
        config.title = "Cheat Game[${chat.username}]"
        gameGUI = CheatGame(playerID, N = N)
        application = LwjglApplication(gameGUI, config)
        while (!gameGUI.loaded) {
            Thread.sleep(200)
        }
        return ""
    }

    override fun getInitialMessage(): String {
        return keyManager.getPublicKey()
    }

    override fun getResult() {
    }

    override fun isFinished(): Boolean {
        return  state == State.END
    }


    override fun close() {
        application.stop()
    }
}