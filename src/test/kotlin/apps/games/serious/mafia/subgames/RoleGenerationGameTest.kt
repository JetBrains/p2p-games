package apps.games.serious.mafia.subgames

import apps.chat.Chat
import apps.games.GameManagerClass
import apps.games.serious.mafia.roles.Role
import apps.games.serious.mafia.subgames.role.generation.RoleGenerationGame
import broker.NettyGroupBroker
import entity.Group
import entity.User
import network.ConnectionManagerClass
import org.apache.log4j.BasicConfigurator
import org.bouncycastle.jce.ECNamedCurveTable
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

class RoleGenerationGameTest {
    lateinit var chats: Array<Chat>
    lateinit var groups: Array<Group>

    private val ECParams = ECNamedCurveTable.getParameterSpec("secp256k1")

    companion object {
        init {
            BasicConfigurator.configure()
        }

        val userClientAdresses = Array(MAX_USERS, { i -> InetSocketAddress("127.0.0.1", 1231 + 2 * i) })
        val userHostAdresses = Array(MAX_USERS, { i -> InetSocketAddress("127.0.0.1", 1232 + 2 * i) })

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
    fun initGame() {
        chats = Array(MAX_USERS, { i -> Mockito.mock(Chat::class.java) ?: throw AssertionError("Initialization error") })
        groups = Array(MAX_USERS, { i -> Group(mutableSetOf(*users)) })

        for (i in 0..MAX_USERS - 1) {
            chats[i].username = "TestUser $i"

            Mockito.`when`(chats[i].me()).thenReturn(users[i])
            Mockito.doNothing().`when`(chats[i]).showMessage(Mockito.anyString())
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
    fun ShuffleTest() {
        val m = Math.sqrt(MAX_USERS * 1.0).toInt()
        val rolesCount = mutableMapOf<Role, Int>()
        for (role in Role.values()) {
            when (role) {
                Role.MAFIA -> rolesCount[role] = m
                else -> if (role != Role.INNOCENT && role != Role.UNKNOWN) rolesCount[role] = 1
            }
        }
        rolesCount[Role.INNOCENT] = MAX_USERS - rolesCount.values.sum()
        val userRolesDeckGames = Array(MAX_USERS, { i -> RoleGenerationGame(chats[i], groups[i], "RoleDeck", ECParams, rolesCount, gameManagers[i]) })
        val futureRoleDecks = Array(MAX_USERS, { i -> gameManagers[i].initSubGame(userRolesDeckGames[i]) })
        val roleDecks = Array(MAX_USERS, { i -> futureRoleDecks[i].get().first })
        val verifiers = Array(MAX_USERS, { i -> futureRoleDecks[i].get().second })
        for (roleDeck in roleDecks) {
            for (otherDeck in roleDecks) {
                roleDeck.shuffledRoles.decryptSeparate(otherDeck.roleKeys)
                roleDeck.V.decryptSeparate(otherDeck.VKeys)
            }
        }
        val roles = mutableListOf<Int>()
        for (i in 0..MAX_USERS - 1) {
            assertTrue(roleDecks[i].originalRoles.contains(roleDecks[i].shuffledRoles.cards[i]))
            roles.add(roleDecks[i].originalRoles.cards.indexOf(roleDecks[i].shuffledRoles.cards[i]))
        }
        for (i in 0..MAX_USERS - 1) {
            for (j in 0..MAX_USERS - 1) {
                val d1 = roleDecks[i].clone()
                val d2 = roleDecks[i].clone()
                val er1 = ArrayList(d1.encryptedR)
                val er2 = ArrayList(d2.encryptedR)
                for (k in 0..MAX_USERS - 1) {
                    for (l in 0..er1.size - 1) {
                        er1[k] *= roleDecks[l].Rkeys.elementAt(k).modInverse(ECParams.n)
                        er2[k] *= roleDecks[l].Rkeys.elementAt(k).modInverse(ECParams.n)
                        er1[k] %= ECParams.n
                        er2[k] %= ECParams.n
                    }
                }
                d1.V.decryptSeparate(er1)
                d2.V.decryptSeparate(er2)
                assertEquals(d1.V, d2.V)
            }
        }
        val roleKeys = mutableMapOf<User, Collection<BigInteger>>()
        val VKeys = mutableMapOf<User, Collection<BigInteger>>()
        val Rkeys = mutableMapOf<User, Collection<BigInteger>>()
        val Xs = mutableMapOf<User, Collection<BigInteger>>()
        for (i in 0..MAX_USERS - 1) {
            roleKeys[users[i]] = roleDecks[i].roleKeys
            VKeys[users[i]] = roleDecks[i].VKeys
            Rkeys[users[i]] = roleDecks[i].Rkeys
            Xs[users[i]] = roleDecks[i].X
        }
        for (verifier in verifiers) {
            assertTrue(verifier.verify(roleKeys, VKeys, Rkeys, Xs))
        }
    }
}