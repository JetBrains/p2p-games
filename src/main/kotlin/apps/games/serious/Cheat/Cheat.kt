package apps.games.serious.Cheat

import apps.chat.Chat
import apps.games.Game
import apps.games.GameExecutionException
import apps.games.serious.TableGame
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
        END,
    }

    private var state: State = State.INIT
    private val keyManager = RSAKeyManager()
    private val HANDSHAKE_PHRASE = "HANDSHAKE" // who would've thought
    private val N = group.users.size

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        for(msg in responses){
            chat.showMessage(msg.value)
        }
        when(state){

            State.INIT -> {
                for(msg in responses){
                    keyManager.registerUser(User(msg.user), msg.value)
                }
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
            State.END -> TODO()
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