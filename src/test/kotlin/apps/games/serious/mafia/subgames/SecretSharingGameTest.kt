package apps.games.serious.mafia.subgames

import apps.chat.Chat
import apps.games.GameManagerClass
import apps.games.serious.mafia.roles.DetectiveRole
import apps.games.serious.mafia.roles.Role
import apps.games.serious.mafia.subgames.role.distribution.RoleDistributionGame
import apps.games.serious.mafia.subgames.role.generation.RoleGenerationGame
import apps.games.serious.mafia.subgames.role.secret.SecretSharingGame
import broker.NettyGroupBroker
import crypto.random.randomBigInt
import entity.Group
import entity.User
import network.ConnectionManagerClass
import org.apache.log4j.BasicConfigurator
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.math.ec.ECPoint
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito
import java.math.BigInteger
import java.net.InetSocketAddress

/**
 * Created by user on 8/25/16.
 */

class SecretSharingGameTest {
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
     * Check, that secret is shared correct:
     *
     * All ids are as they should be
     * All secrets sum up t orequired values
     */
    @Test
    fun testSecretSharing() {
        val m = Math.sqrt(MAX_USERS * 1.0).toInt()
        val rolesCount = mutableMapOf<Role, Int>()
        for (role in Role.values()) {
            when (role) {
                Role.MAFIA -> rolesCount[role] = m
                else -> if (role != Role.INNOCENT && role != Role.UNKNOWN) rolesCount[role] = 1
            }
        }
        rolesCount[Role.INNOCENT] = MAX_USERS - rolesCount.values.sum()
        val userRolesDeckGames = Array(MAX_USERS, { i -> RoleGenerationGame(chats[i], groups[i], "RoleDeckRoles", ECParams, rolesCount, gameManagers[i]) })
        val futureRoleDecks = Array(MAX_USERS, { i -> gameManagers[i].initSubGame(userRolesDeckGames[i]) })
        val roleDecks = Array(MAX_USERS, { i -> futureRoleDecks[i].get().first })
        val roleDistributionGames = Array(MAX_USERS, { i -> RoleDistributionGame(chats[i], groups[i], "RoleDistribution", ECParams, roleDecks[i], gameManagers[i]) })
        val futureRoles = Array(MAX_USERS, { i -> gameManagers[i].initSubGame(roleDistributionGames[i]) })
        val roles = Array(MAX_USERS, { i -> futureRoles[i].get().first })
        val ids = Array(MAX_USERS, { i -> randomBigInt(ECParams.n) })
        val secretSharingGames = Array(MAX_USERS, { i -> SecretSharingGame(chats[i], groups[i], "SecretSharingGame", ECParams, roles[i], ids[i], gameManagers[i]) })
        val futuresSecrets = Array(MAX_USERS, { i -> gameManagers[i].initSubGame(secretSharingGames[i]) })
        val secrets = Array(MAX_USERS, { i -> futuresSecrets[i].get().first })
        val detective: DetectiveRole = (roles.first { x -> x.role == Role.DETECTIVE } as DetectiveRole)
        for (i in 0..MAX_USERS - 1) {
            var sum: ECPoint = ECParams.curve.infinity
            for (secret in secrets) {
                sum = sum.add(secret.secrets.cards[i])
            }
            var f: Boolean = false
            var target: Int = 0
            for (j in 0..MAX_USERS - 1) {
                if (sum == ECParams.g.multiply(ids[j]) || (sum == ECParams.g.multiply(ids[j] * BigInteger.valueOf(2)))) {
                    f = true
                    target = j
                }
            }
            //Assert at least one user found
            assertTrue(f)
            //Assert all encrypted IDs are correct
            val k = detective.getUserK(users[target])
            assertEquals(1, secrets.map { x -> x.ids.cards[i] }.distinct().size)
            assertEquals(ECParams.g.multiply(ids[target] * k), secrets[0].ids.cards[i])
            //check sum
            val multiplier = if (roles[target].role == Role.MAFIA) ids[target] * BigInteger.valueOf(2) else ids[target]
            assertEquals(ECParams.g.multiply(multiplier), sum)

        }
    }


}
