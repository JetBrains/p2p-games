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
import java.net.InetSocketAddress
import java.net.InterfaceAddress
import javax.swing.SwingUtilities
import javax.swing.UIManager


/**
 * Created by user on 6/20/16.
 */

fun proxyListener(x: ChatMessageProto.ChatMessage): GenericMessageProto.GenericMessage?{
    print("I am here")
    return null
}

fun main(args : Array<String>) {
    println("Hello, world!")


    BasicConfigurator.configure();
    val a1: InetSocketAddress = InetSocketAddress("localhost", 1231)
    val a2: InetSocketAddress = InetSocketAddress("localhost", 1232)
    val a3: InetSocketAddress = InetSocketAddress("localhost", 1233)
    val a4: InetSocketAddress = InetSocketAddress("localhost", 1234)
    val connectionManager = ConnectionManager(a1, a2)
    val chat = Chat(ChatGUI(666), connectionManager)

    val gui = Thread(chat)
    gui.start()
    gui.join()
    //connectionManager.close()
}