import apps.chat.GUI.ChatGUI
import network.ConnectionManager
import network.dispatching.EnumDispatcher
import network.dispatching.SimpleDispatcher
import network.MessageClient
import network.MessageServer
import org.apache.log4j.BasicConfigurator
import proto.ChatMessageProto
import proto.GenericMessageProto
import apps.chat.Chat
import proto.EntitiesProto
import proto.QueryProto
import java.net.InetSocketAddress
import java.net.InterfaceAddress
import javax.swing.SwingUtilities
import javax.swing.UIManager

/**
 * Created by user on 6/20/16.
 */


fun main(args : Array<String>) {
    println("Hello, world!")


    BasicConfigurator.configure();
    val a1: InetSocketAddress = InetSocketAddress("localhost", 1231)
    val a2: InetSocketAddress = InetSocketAddress("localhost", 1232)
    val a3: InetSocketAddress = InetSocketAddress("localhost", 1233)
    val a4: InetSocketAddress = InetSocketAddress("localhost", 1234)

    val connectionManager = ConnectionManager(a3, a4)


    //sample message
    val sampleBuilder = ChatMessageProto.ChatMessage.newBuilder()
    sampleBuilder.chatId = 666
    sampleBuilder.message = "Need more Souls"
    sampleBuilder.user = (EntitiesProto.User.newBuilder().setHostname(a4.hostName).setPort(a4.port).setName("Mark Geller").build())
    val sample = GenericMessageProto.GenericMessage.newBuilder().
            setType(GenericMessageProto.GenericMessage.Type.CHAT_MESSAGE).setChatMessage(sampleBuilder).build()

    //sample query
    val chatQuery = QueryProto.ChatMemberQuery.newBuilder().setChatID(666).build()
    val query = QueryProto.Query.newBuilder().setQuery(chatQuery).setType(QueryProto.Query.Type.CHAT_MEMBER_QUERY).build()
    val genericMessage = GenericMessageProto.GenericMessage.newBuilder()
            .setType(GenericMessageProto.GenericMessage.Type.QUERY).setQuery(query).build()

    val chat = Chat(ChatGUI(666), connectionManager)
    //connectionManager.send(a2, sample)
    connectionManager.send(a2, genericMessage)

    //TODO Message response (for directed queries)
//    val gui = Thread(chat)
//    gui.start()
//    gui.join()

}
