import GUI.ChatGUI
import network.ConnectionManager
import network.dispatching.EnumDispatcher
import network.dispatching.SimpleDispatcher
import network.MessageClient
import network.MessageServer
import org.apache.log4j.BasicConfigurator
import proto.ChatMessageProto
import proto.GenericMessageProto
import service.chat.ChatService
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


//    //sample message
    val sampleBuilder = ChatMessageProto.ChatMessage.newBuilder()
    sampleBuilder.chatId = 666
    sampleBuilder.message = "Need more Souls"
    sampleBuilder.user = (ChatMessageProto.User.newBuilder().setHostname(a4.hostName).setPort(a4.port).setName("Mark Geller").build())
    val sample = GenericMessageProto.GenericMessage.newBuilder().
            setType(GenericMessageProto.GenericMessage.Type.CHAT_MESSAGE).setChatMessage(sampleBuilder).build()

    val chat = ChatService(ChatGUI(666), connectionManager)
    connectionManager.send(a2, sample)
    val gui = Thread(chat)
    gui.start()
    gui.join()

}
