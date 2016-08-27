package apps.games.serious.mafia.subgames

import apps.chat.Chat
import apps.games.GameManagerClass
import apps.games.serious.mafia.roles.Role
import apps.games.serious.mafia.subgames.role.distribution.RoleDistributionGame
import apps.games.serious.mafia.subgames.role.generation.RoleGenerationGame
import broker.NettyGroupBroker
import entity.Group
import entity.User
import network.ConnectionManagerClass
import org.apache.log4j.BasicConfigurator
import org.bouncycastle.jce.ECNamedCurveTable
import org.junit.AfterClass
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.Assert.*
import org.mockito.Mockito
import java.net.InetSocketAddress

/**
 * Created by user on 8/24/16.
 */


class RoleDistributionGameTest{
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
     * Create a role deck, distribute roles and verify, that number
     * of occurrences of each role is exactly as was planned
     */
    @Test
    fun testRoles(){
        val m = Math.sqrt(MAX_USERS * 1.0).toInt()
        val rolesCount = mutableMapOf<Role, Int>()
        for(role in Role.values()){
            when(role){
                Role.MAFIA -> rolesCount[role] = m
                else -> if(role != Role.INNOCENT && role != Role.UNKNOWN) rolesCount[role] = 1
            }
        }
        rolesCount[Role.INNOCENT] = MAX_USERS - rolesCount.values.sum()
        val userRolesDeckGames = Array(MAX_USERS, { i -> RoleGenerationGame(chats[i], groups[i], "RoleDeckRoles", ECParams, rolesCount, gameManagers[i]) })
        val futureRoleDecks = Array(MAX_USERS, {i -> gameManagers[i].initSubGame(userRolesDeckGames[i])})
        val roleDecks = Array(MAX_USERS, {i -> futureRoleDecks[i].get().first})
        val roleDistributionGames = Array(MAX_USERS, { i -> RoleDistributionGame(chats[i], groups[i], "RoleDistribution", ECParams, roleDecks[i], gameManagers[i]) })
        val futureRoles = Array(MAX_USERS, {i -> gameManagers[i].initSubGame(roleDistributionGames[i])})
        val roles = Array(MAX_USERS, {i -> futureRoles[i].get()})
        val entriesCount = roles.associate { x -> x.role to roles.count { t -> t.role == x.role } }
        assertEquals(rolesCount, entriesCount)
    }

    /**
     * Create a role deck, distribute roles and verify, that number
     * of occurrences of each role is exactly as was planned
     */
    @Test
    fun testComrades(){
        val m = Math.sqrt(MAX_USERS * 1.0).toInt()
        val rolesCount = mutableMapOf<Role, Int>()
        for(role in Role.values()){
            when(role){
                Role.MAFIA -> rolesCount[role] = m
                else -> if(role != Role.INNOCENT && role != Role.UNKNOWN) rolesCount[role] = 1
            }
        }
        rolesCount[Role.INNOCENT] = MAX_USERS - rolesCount.values.sum()
        val userRolesDeckGames = Array(MAX_USERS, { i -> RoleGenerationGame(chats[i], groups[i], "RoleDeckComrades", ECParams, rolesCount, gameManagers[i]) })
        val futureRoleDecks = Array(MAX_USERS, {i -> gameManagers[i].initSubGame(userRolesDeckGames[i])})
        val roleDecks = Array(MAX_USERS, {i -> futureRoleDecks[i].get().first})
        val roleDistributionGames = Array(MAX_USERS, { i -> RoleDistributionGame(chats[i], groups[i], "RoleDistribution", ECParams, roleDecks[i], gameManagers[i]) })
        val futureRoles = Array(MAX_USERS, {i -> gameManagers[i].initSubGame(roleDistributionGames[i])})
        val roles = Array(MAX_USERS, {i -> futureRoles[i].get()})
        for(role in roles){
            if(role.role == Role.INNOCENT){
                assertEquals(1, role.getComrades().size)
            }else{
                assertEquals("${role.role}", rolesCount[role.role], role.getComrades().size)
            }
        }
    }
}
