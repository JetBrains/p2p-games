package apps.games.serious.mafia.subgames

import apps.chat.Chat
import apps.games.GameManagerClass
import apps.games.primitives.Deck

import broker.NettyGroupBroker
import crypto.random.randomPermutation
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
 * Created by user on 8/11/16.
 */
val MAX_USERS = 9

class RoleGenerationGameTest{
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
            Mockito.doReturn(NettyGroupBroker(connectionManagers[i])).`when`(chats[i]).groupBroker
            Mockito.doReturn(groups[i]).`when`(chats[i]).group
        }
    }

    /**
     * Create a common random deck. Then shuffle
     * it. For each user generate a random order
     * of decryption keys. Verify, that deck is
     * known only after all MAX_USERS keys ar
     * known
     */
    @Test
    fun ShuffleTest(){
        val m = Math.sqrt(MAX_USERS * 1.0).toInt()
        val rolesCount = IntArray(MAX_USERS - m + 1, {i -> if (i == 0) m else 1}).toList()
        val userRolesDeckGames = Array(MAX_USERS, { i -> RoleGenerationGame(chats[i], groups[i], "RoleDeck", ECParams, rolesCount, gameManagers[i]) })
        val futureRoleDecks = Array(MAX_USERS, {i -> gameManagers[i].initSubGame(userRolesDeckGames[i])})
        val roleDecks = Array(MAX_USERS, {i -> futureRoleDecks[i].get().first})
        val verifiers = Array(MAX_USERS, {i -> futureRoleDecks[i].get().second})
        for(roleDeck in roleDecks){
            for(otherDeck in roleDecks){
                roleDeck.shuffledRoles.decryptSeparate(otherDeck.roleKeys)
                roleDeck.V.decryptSeparate(otherDeck.VKeys)
            }
        }
        val roles = mutableListOf<Int>()
        for(i in 0..MAX_USERS-1) {
            assertTrue(roleDecks[i].originalRoles.contains(roleDecks[i].shuffledRoles.cards[i]))
            roles.add(roleDecks[i].originalRoles.cards.indexOf(roleDecks[i].shuffledRoles.cards[i]))
        }
        for(i in 0..MAX_USERS-1){
            for(j in 0..MAX_USERS-1){
                val R1 = Array(MAX_USERS, {t -> roleDecks[i].V.cards[i].multiply(roleDecks[i].commonR.elementAt(t).modInverse(ECParams.n))})
                val R2 = Array(MAX_USERS, {t -> roleDecks[j].V.cards[j].multiply(roleDecks[j].commonR.elementAt(t).modInverse(ECParams.n))})
                if((roles[i] < m && roles[j] < m) || i == j){
                    assertTrue(R1.intersect(R2.toList()).isNotEmpty())
                    println("${roles[i]} ${roles[j]}")
                    println(R1.intersect(R2.toList()).size)
                }else{
                    assertTrue(R1.intersect(R2.toList()).isEmpty())
                }
            }
        }
        val roleKeys = mutableMapOf<User, Collection<BigInteger>>()
        val VKeys = mutableMapOf<User, Collection<BigInteger>>()
        val Rs = mutableMapOf<User, Collection<BigInteger>>()
        val Xs = mutableMapOf<User, Collection<BigInteger>>()
        for(i in 0..MAX_USERS-1){
            roleKeys[users[i]]  = roleDecks[i].roleKeys
            VKeys[users[i]] = roleDecks[i].VKeys
            Rs[users[i]] = roleDecks[i].ownR
            Xs[users[i]] = roleDecks[i].X
        }
        for(verifier in verifiers){
            assertTrue(verifier.verify(roleKeys, VKeys, Rs, Xs))
        }
    }
}