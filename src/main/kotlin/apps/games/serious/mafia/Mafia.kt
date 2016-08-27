package apps.games.serious.mafia

import apps.chat.Chat
import apps.games.GameExecutionException
import apps.games.serious.CardGame
import apps.games.serious.Cheat.GUI.CheatGame
import apps.games.serious.mafia.GUI.MafiaGame
import apps.games.serious.mafia.roles.PlayerRole
import apps.games.serious.mafia.roles.Role
import apps.games.serious.mafia.subgames.role.distribution.RoleDistributionGame
import apps.games.serious.mafia.subgames.role.distribution.RoleDistributionHelper
import apps.games.serious.mafia.subgames.role.generation.RoleGenerationGame
import apps.games.serious.mafia.subgames.role.generation.RoleGenerationVerifier
import apps.games.serious.mafia.subgames.role.secret.SecretDeck
import apps.games.serious.mafia.subgames.role.secret.SecretSharingGame
import apps.games.serious.mafia.subgames.role.secret.SecretSharingVerifier
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import crypto.RSA.ECParams
import crypto.RSA.RSAKeyManager
import crypto.random.randomBigInt
import crypto.random.randomString
import entity.Group
import entity.User
import org.bouncycastle.crypto.InvalidCipherTextException
import proto.GameMessageProto
import java.math.BigInteger
import java.util.concurrent.CancellationException


/**
 * Created by user on 8/9/16.
 */

class Mafia(chat: Chat, group: Group, gameID: String) : CardGame(chat, group, gameID, 36) {
    override val name: String
        get() = "mafia game"

    private enum class State{
        INIT,
        VALIDATE_KEYS,
        INIT_ID,
        LOOP,
        END,
    }

    private lateinit var gameGUI: MafiaGame
    private lateinit var application: LwjglApplication

    private var state: State = State.INIT
    private val keyManager = RSAKeyManager()
    private val HANDSHAKE_PHRASE = "HANDSHAKE" // who would've thought
    private lateinit var role: PlayerRole
    private lateinit var secretDeck: SecretDeck
    private lateinit var roleGenerationVerifier: RoleGenerationVerifier
    private lateinit var secretSharingVerivier: SecretSharingVerifier
    private val ids: Array<BigInteger>

    private val N: Int
    private val M: Int
    init {
        N = group.users.size
        ids = Array(N, {i -> BigInteger.ZERO})
        //if(N < 3) throw GameExecutionException("mafia is only viable with 4+ players")
        M = Math.sqrt(N * 1.0).toInt()
        initGame()
    }



    override fun evaluate(responses: List<GameMessageProto.GameStateMessage>): String {
        for (msg in responses) {
            chat.showMessage("[${msg.user.name}] said ${msg.value}")
        }
        when(state){

            State.INIT -> {
                for (msg in responses) {
                    keyManager.registerUserPublicKey(User(msg.user), msg.value)
                }
                currentPlayerID = -1
                state = State.VALIDATE_KEYS
            }
            State.VALIDATE_KEYS -> {
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
                    state = State.INIT_ID
                    chat.sendMessage("RSA is OK. Generating deck")
                    return randomBigInt(ECParams.n).toString()

                }else{
                    val s = randomString(512) + " " + HANDSHAKE_PHRASE
                    return keyManager.encodeForUser(playerOrder[currentPlayerID], s)
                }
            }
            State.INIT_ID -> {
                for(msg in responses){
                    val user = User(msg.user)
                    val userID = playerOrder.indexOf(user)
                    ids[userID] = BigInteger(msg.value)
                }
                for(i in 0..N-1){
                    for(j in 0..N-1){
                        if((i != j && ids[i] == ids[j]) || ids[i] == BigInteger.valueOf(2) * ids[j]){
                            return randomBigInt(ECParams.n).toString()
                        }
                    }
                }
                state = State.LOOP

            }
            State.LOOP -> {
                Thread.sleep(2000)
            }
            State.END -> TODO()
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
        gameGUI = MafiaGame(group)
        application = LwjglApplication(gameGUI, config)
        while (!gameGUI.loaded) {
            Thread.sleep(200)
        }
        initRoles()
        return ""
    }

    /**
     * process role generation and distribution.
     * after this step [role] holds current player role
     * with the list of known comrades
     */
    private fun initRoles(){
        val rolesCount = mutableMapOf<Role, Int>()
        for(role in Role.values()){
            when(role){
                Role.MAFIA -> rolesCount[role] = M
                else -> if(role != Role.INNOCENT && role != Role.UNKNOWN) rolesCount[role] = 1
            }
        }
        rolesCount[Role.INNOCENT] = N - rolesCount.values.sum()
        val roleGenerationGame = RoleGenerationGame(chat, group, subGameID(), ECParams, rolesCount, gameManager)
        try {
            val roleGenerationFuture = runSubGame(roleGenerationGame)
            roleGenerationVerifier = roleGenerationFuture.get().second
            val roleFuture = runSubGame(RoleDistributionGame(chat, group, subGameID(), ECParams, roleGenerationFuture.get().first, gameManager))
            role = roleFuture.get()
        }catch (e: CancellationException){
            return
        }catch (e: Exception){
            throw GameExecutionException(e.message ?: "Something went wrong in subgame")
        }
    }

    private fun shareSecrets(){
        try {
            val secretFuture = runSubGame(SecretSharingGame(chat, group, subGameID(), ECParams, role, ids[playerID], gameManager))
            secretDeck = secretFuture.get().first
            secretSharingVerivier = secretFuture.get().second
        } catch (e: CancellationException){

        }
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