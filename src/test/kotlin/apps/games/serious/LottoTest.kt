package apps.games.serious

import apps.chat.Chat
import apps.games.GameManager
import broker.GroupBroker
import broker.NettyGroupBroker
import entity.ChatMessage
import entity.Group
import entity.User
import network.ConnectionManager
import org.apache.log4j.BasicConfigurator
import org.junit.AfterClass
import org.junit.Assert.*
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.Mockito.*
import proto.GameMessageProto
import kotlin.reflect.KFunction

/**
 * Created by user on 6/30/16.
 * Tests are straightforward:
 * create lotto game, send it expected messages,
 * until desired condition is met
 */
class LottoTest{
    var lotto: Lotto? = null
    var chat: Chat? = null
    var group: Group? = null


    companion object {
        val sampleTicket = Ticket.from(5, 30, "1 2 3 4 5")
        val sampleUser = User(Settings.hostAddress, "TestUser")

        @BeforeClass @JvmStatic fun setup() {
            BasicConfigurator.configure();
            GameManager.start()
        }

        @AfterClass @JvmStatic fun teardown() {
            GameManager.close()
        }
    }

    @Before
    fun initGame(){
        chat = mock(Chat::class.java) ?: throw AssertionError("Initialization error")
        (chat as Chat).username = "TestUser"
        group = mock(Group::class.java) ?: throw AssertionError("Initialization error")

        doReturn("1 2 3 4 5").`when`(chat)?.getUserInput(anyString(), any(Ticket.getValidator(5, 30).javaClass))
        doReturn(NettyGroupBroker()).`when`(chat)?.groupBroker
        doReturn(group).`when`(chat)?.group

        doReturn(mutableSetOf(sampleUser)).`when`(group)?.users
        doReturn(Group(mutableSetOf(sampleUser))).`when`(group)?.clone()
        lotto = Lotto(chat as Chat, group as Group, "TestGame")

    }

    /**
     * Play a game of lotto with ourself.
     * We should win this one
     */
    @Test
    fun winTest(){
        if(lotto == null){
            throw AssertionError("Initialization error")
        }
        //perform handshake
        (lotto as Lotto).evaluate(listOf<GameMessageProto.GameStateMessage>())
        //agree on tickets
        (lotto as Lotto).evaluate(listOf<GameMessageProto.GameStateMessage>())
        //run until we won
        while(!(lotto as Lotto).isFinished()){
            (lotto as Lotto).evaluate(listOf<GameMessageProto.GameStateMessage>())
        }
        // We are the only player - we should win
        assertTrue("Ticket is incorrect", (lotto as Lotto).verifyTicket(sampleTicket))
        assertTrue("Somehow we lost to ourself", (lotto as Lotto).win())
    }

    /**
     * Run a game of lotto for four turns.
     * Try to claim, that we won -> Fail
     */
    @Test
    fun cheatTest(){
        if(lotto == null){
            throw AssertionError("Initialization error")
        }
        //perform handshake
        (lotto as Lotto).evaluate(listOf<GameMessageProto.GameStateMessage>())
        //agree on tickets
        (lotto as Lotto).evaluate(listOf<GameMessageProto.GameStateMessage>())
        //run until we won
        for(i in 1..4){
            (lotto as Lotto).evaluate(listOf<GameMessageProto.GameStateMessage>())
        }
        assertFalse("We had only four rounds. How can You win?!", (lotto as Lotto).verifyTicket(sampleTicket))
    }

}