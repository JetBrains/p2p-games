package apps.games.serious.mafia.subgames.sum

import apps.chat.Chat
import apps.games.GameManagerClass
import apps.games.serious.mafia.roles.Role
import apps.games.serious.mafia.subgames.MAX_USERS
import apps.games.serious.mafia.subgames.role.generation.RoleGenerationGame
import broker.NettyGroupBroker
import crypto.RSA.RSAKeyManager
import crypto.random.randomBigInt
import entity.Group
import entity.User
import network.ConnectionManagerClass
import org.apache.log4j.BasicConfigurator
import org.bouncycastle.jce.ECNamedCurveTable
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.math.BigInteger
import java.net.InetSocketAddress
import java.util.*

/**
 * Created by user on 8/25/16.
 */
class SecureMultipartySumGameTest{
    lateinit var chats: Array<Chat>
    lateinit var groups: Array<Group>

    private val ECParams = ECNamedCurveTable.getParameterSpec("secp256k1")

    companion object {
        init{
            BasicConfigurator.configure()
        }
        val userClientAdresses = Array(MAX_USERS, { i -> InetSocketAddress("127.0.0.1", 1231 + 2*i) })
        val userHostAdresses = Array(MAX_USERS, { i -> InetSocketAddress("127.0.0.1", 1232 + 2*i) })

        val users = Array(MAX_USERS, { i -> User(userHostAdresses[i], "TestUser $i") })

        val connectionManagers = Array(MAX_USERS, { i -> ConnectionManagerClass(userClientAdresses[i], userHostAdresses[i]) })

        val gameManagers = Array(MAX_USERS, { i -> GameManagerClass(connectionManagers[i]) })



        @BeforeClass @JvmStatic fun setup() {
            gameManagers.forEach { x -> x.start() }
        }

        @AfterClass @JvmStatic fun teardown() {
            gameManagers.forEach { x -> x.close() }
            //            connectionManagers.forEach { x -> x.close() }
        }
    }

    @Before
    fun initGame(){
        chats = Array(MAX_USERS, { i -> Mockito.mock(Chat::class.java) ?: throw AssertionError("Initialization error")})
        groups = Array(MAX_USERS, { i -> Group(mutableSetOf(*users)) })

        for(i in 0..MAX_USERS -1){
            chats[i].username = "TestUser $i"

            Mockito.`when`(chats[i].me()).thenReturn(users[i])
            Mockito.doNothing().`when`(chats[i]).showMessage(Mockito.anyString())
            Mockito.doReturn(NettyGroupBroker(connectionManagers[i])).`when`(chats[i]).groupBroker
            Mockito.doReturn(groups[i]).`when`(chats[i]).group
        }
    }

    /**
     * Run SMS on random inputs.
     *
     * verify, that sum is computed correctly
     * verify, that all verifiers work correctly
     * verify, that initial inputs are restored appropriately
     */
    @Test
    fun checkSMSVerifier(){
        val inputs = Array(MAX_USERS, {i -> randomBigInt(ECParams.n)})
        var sum: BigInteger = BigInteger.ZERO
        for(i in 0..MAX_USERS-1){
            sum += inputs[i]
        }
        val keyManagers = Array(MAX_USERS, {i -> RSAKeyManager()})
        val SMSGames = Array(MAX_USERS, {i -> SecureMultipartySumGame(chats[i], groups[i], "SMSTest1", keyManagers[i], inputs[i], gameManagers[i])})
        val sumFutures = Array(MAX_USERS, {i -> gameManagers[i].initSubGame(SMSGames[i])})
        val sums = Array(MAX_USERS, {i -> sumFutures[i].get().first})
        val verifiers = Array(MAX_USERS, {i -> sumFutures[i].get().second})
        assertEquals(sum, sums[0])
        assertEquals(1, sums.distinct().size)
        for(keyManager in keyManagers){
            for(i in 0..MAX_USERS-1){
                keyManager.registerUserPrivateKey(users[i], keyManagers[i].getPrivateKey())
            }
        }
        for(i in 0..MAX_USERS-1){
            assertTrue(verifiers[i].verifySums(keyManagers[i]))
            val computedInputs = verifiers[i].getInputs(keyManagers[i])
            for(j in 0..MAX_USERS-1){
                assertEquals(inputs[j], computedInputs[users[j]])
            }
        }
    }
}