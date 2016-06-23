import apps.chat.ChatManager
import network.ConnectionManager
import org.apache.log4j.BasicConfigurator
import java.net.InetSocketAddress

/**
 * Created by user on 6/20/16.
 */


fun main(args: Array<String>) {
    println("Hello, world!")


    BasicConfigurator.configure();
    val a1: InetSocketAddress = InetSocketAddress("localhost", 1231)
    val a2: InetSocketAddress = InetSocketAddress("localhost", 1232)
    val a3: InetSocketAddress = InetSocketAddress("localhost", 1235)
    val a4: InetSocketAddress = InetSocketAddress("localhost", 1236)

    val connectionManager = ConnectionManager(a3, a4)

    val chatManager = ChatManager(connectionManager)
    chatManager.joinChat(666, a2)

}
