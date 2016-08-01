package apps.games.serious.Cheat

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.serious.Cheat.GUI.CheatGame
import apps.games.serious.TableGame
import apps.games.serious.preferans.GUI.PreferansGame
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import crypto.RSA.RSAKeyManager
import crypto.random.randomString
import entity.Group
import entity.User
import org.bouncycastle.crypto.InvalidCipherTextException
import proto.GameMessageProto

/**
 * Created by user on 7/28/16.
 */

class Cheat(chat: Chat, group: Group, gameID: String) :
                                            TableGame(chat, group, gameID) {
    override val name: String
        get() = "Cheat Game "

    private enum class State{
        INIT,
        VALIDATE_KEYS,
        GENERATE_DECK,
        END,
    }

    private var state: State = State.INIT

    private lateinit var gameGUI: CheatGame
    private lateinit var application: LwjglApplication

    private val keyManager = RSAKeyManager()
    private val HANDSHAKE_PHRASE = "HANDSHAKE" // who would've thought
    private val N = group.users.size
    private var DECK_SIZE: Int = 36


    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        for(msg in responses){
            chat.showMessage(msg.value)
        }
        when(state){

            State.INIT -> {
                for(msg in responses){
                    keyManager.registerUser(User(msg.user), msg.value)
                }
                initGame()
                currentPlayerID = -1
                state = State.VALIDATE_KEYS
            }
            State.VALIDATE_KEYS -> {
                if(playerID == currentPlayerID){
                    for(msg in responses){
                        try {
                            val s = keyManager.decodeString(msg.value)
                            val handshake = s.split(" ").last()
                            if(handshake != HANDSHAKE_PHRASE){
                                throw GameExecutionException("Invalid RSA key")
                            }
                        }catch (e: InvalidCipherTextException){
                            throw GameExecutionException("Malformed RSA key")
                        }
                    }
                }

                currentPlayerID ++
                if(currentPlayerID == N){
                    state = State.END
                    chat.sendMessage("RSA is OK. Generating deck")
                    return ""
                }
                val s = randomString(512) + " " + HANDSHAKE_PHRASE
                return keyManager.encodeForUser(playerOrder[currentPlayerID], s)

            }
            State.GENERATE_DECK -> {

            }
            State.END -> TODO()
        }
        return ""
    }


    /**
     * Start GUI for the Preferans game
     */
    private fun initGame(): String {
        val config = LwjglApplicationConfiguration()
        config.width = 1024
        config.height = 1024
        config.forceExit = false
        config.title = "Cheate Game[${chat.username}]"
        gameGUI = CheatGame(playerID)
        application = LwjglApplication(gameGUI, config)
        while (!gameGUI.loaded) {
            Thread.sleep(200)
        }
        return ""
    }

    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun getInitialMessage(): String {
        return keyManager.getPublicKey()
    }

    override fun getResult() {}


}