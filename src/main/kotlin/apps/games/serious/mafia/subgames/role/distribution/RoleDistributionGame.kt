package apps.games.serious.mafia.subgames.role.distribution

import apps.chat.Chat
import apps.games.Game
import apps.games.GameManager
import apps.games.GameManagerClass
import apps.games.serious.mafia.roles.PlayerRole
import apps.games.serious.mafia.roles.Role
import apps.games.serious.mafia.subgames.role.generation.RoleDeck
import crypto.random.randomString
import entity.Group
import entity.User
import org.bouncycastle.jce.spec.ECParameterSpec
import proto.GameMessageProto
import java.math.BigInteger

/**
 * Created by user on 8/12/16.
 */

class RoleDistributionGame(chat: Chat, group: Group, gameID: String, val ECParams: ECParameterSpec,
                           val roleDeck: RoleDeck, gameManager: GameManagerClass = GameManager) :
                            Game<Pair<PlayerRole, RoleDistributionHelper>>(chat, group, gameID, gameManager = gameManager) {

    override val name: String
        get() = "Role Distribution Game"

    val playerOrder = group.users.sortedBy { x -> x.name }.toMutableList()
    val playerID = playerOrder.indexOf(chat.me())
    val keyHelper = RoleDistributionHelper(roleDeck.clone(), group.users.toList())
    val N = group.users.size
    val HANDSHAKE_PHRASE = "HANDSHAKEREN"
    private val salt = randomString(512)
    private enum class State{
        INIT,
        EXCHANGE_KEYS, //EXCHANGE KEYS 1..N
        VERIFY_COMRADES,
        END
    }

    private var state: State = State.INIT

    private lateinit var role: PlayerRole

    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        for(msg in responses){
            chat.showMessage(msg.value)
        }
        when(state){

            State.INIT -> {
                val keys = mutableListOf<BigInteger>()
                for(i in 0..N-1){
                    if(i != playerID){
                        keys.add(roleDeck.roleKeys.elementAt(i))
                        keys.add(roleDeck.VKeys.elementAt(i))
                        keys.add(roleDeck.Rkeys.elementAt(i))
                    }else{
                        keys.add(BigInteger.ZERO)
                        keys.add(BigInteger.ZERO)
                        keys.add(BigInteger.ZERO)
                    }
                }
                state = State.EXCHANGE_KEYS
                return keys.joinToString(" ")
            }
            State.EXCHANGE_KEYS -> {
                for(msg in responses){
                    val user = User(msg.user)
                    val userID = playerOrder.indexOf(user)
                    val split = msg.value.split(" ").map { x -> BigInteger(x) }
                    for(i in 0..3*N-1 step 3){
                        if(i != 3*userID){
                            keyHelper.registerRoleKey(user, i/3, split[i])
                            keyHelper.registerVKey(user, i/3, split[i+1])
                            keyHelper.registerRKey(user, i/3, split[i+2])
                        }
                    }
                }
                keyHelper.registerRoleKey(chat.me(), playerID, roleDeck.roleKeys.elementAt(playerID))
                keyHelper.registerVKey(chat.me(), playerID, roleDeck.VKeys.elementAt(playerID))
                keyHelper.registerRKey(chat.me(), playerID, roleDeck.Rkeys.elementAt(playerID))

                role = keyHelper.getRole(playerID).playerRole
                role.registerIV(keyHelper.getDecryptedVCard(playerID).normalize())
                state = State.VERIFY_COMRADES
                return role.encryptForComrades(salt + " " + HANDSHAKE_PHRASE)

            }
            State.VERIFY_COMRADES -> {
                for(msg in responses){
                    val user = User(msg.user)
                    val decrypted: String
                    try {
                        decrypted = role.decryptForComrades(msg.value)
                    } catch (e: Exception){
                        decrypted = ""
                        chat.showMessage("${user.name} is not my comrade")
                    }
                    val split = decrypted.split(" ")
                    if(split.size != 2){
                        chat.showMessage("${user.name} is not my comrade")
                    }else if(split[1] == HANDSHAKE_PHRASE){
                        chat.showMessage("${user.name} is my comrade")
                        role.registerComrade(user)
                    }
                }
                state = State.END
            }
            State.END -> TODO()
        }
        return ""
    }


    override fun isFinished(): Boolean {
        return state == State.END
    }

    override fun getResult(): Pair<PlayerRole, RoleDistributionHelper> {
        return role to keyHelper
    }
}