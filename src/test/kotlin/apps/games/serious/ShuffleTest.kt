package apps.games.serious

import apps.chat.Chat
import apps.games.GameManagerClass
import apps.games.primitives.protocols.RandomDeckGame
import broker.NettyGroupBroker
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
import org.mockito.Mockito.*
import java.net.InetSocketAddress

/**
 * Created by user on 6/30/16.
 * Tests are straightforward:
 * create lotto game, send it expected messages,
 * until desired condition is met
 */
val MAX_USERS = 20

class ShuffleTest{

    lateinit var chats: Array<Chat>
    lateinit var groups: Array<Group>

    private val ECParams = ECNamedCurveTable.getParameterSpec("secp256k1")

    companion object {
        init{
            BasicConfigurator.configure()
        }
        val userClientAdresses = Array(MAX_USERS, {i -> InetSocketAddress("127.0.0.1", 1231 + 2*i)})
        val userHostAdresses = Array(MAX_USERS, {i -> InetSocketAddress("127.0.0.1", 1232 + 2*i)})

        val users = Array(MAX_USERS, {i -> User(userHostAdresses[i], "TestUser $i")})

        val connectionManagers = Array(MAX_USERS, {i -> ConnectionManagerClass(userClientAdresses[i], userHostAdresses[i])})

        val gameManagers = Array(MAX_USERS, {i -> GameManagerClass(connectionManagers[i])})



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
        chats = Array(MAX_USERS, {i -> mock(Chat::class.java) ?: throw AssertionError("Initialization error")})
        groups = Array(MAX_USERS, {i -> Group(mutableSetOf(*users)) })

        for(i in 0..MAX_USERS-1){
            chats[i].username = "TestUser $i"
            `when`(chats[i].me()).thenReturn(users[i])
            doReturn(NettyGroupBroker(connectionManagers[i])).`when`(chats[i]).groupBroker
            doReturn(groups[i]).`when`(chats[i]).group
        }



    }

    /**
     * ShuffleTest
     * Create a common random deck, encrypt and
     * shuffle it between two users
     */
    @Test
    fun RandomDeckTest(){
        val userGames = Array(MAX_USERS, {i -> RandomDeckGame(chats[i], groups[i], "RandomDeck", ECParams, gameManager = gameManagers[i])})
        val futures = Array(MAX_USERS, {i -> gameManagers[i].initSubGame(userGames[i])})
        for(i in 1..MAX_USERS-1){
            assertEquals(futures[0].get(), futures[i].get())
        }

    }


}