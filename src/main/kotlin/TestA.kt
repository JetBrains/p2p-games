import apps.chat.ChatManager
import network.ConnectionManager
import org.apache.log4j.BasicConfigurator
import proto.ChatMessageProto
import proto.GenericMessageProto
import java.net.InetSocketAddress


/**
 * Created by user on 6/20/16.
 */

fun proxyListener(x: ChatMessageProto.ChatMessage): GenericMessageProto.GenericMessage? {
    print("I am here")
    return null
}

fun main(args: Array<String>) {
    println("Hello, world!")


    BasicConfigurator.configure();
    val a1: InetSocketAddress = InetSocketAddress("localhost", 1231)
    val a2: InetSocketAddress = InetSocketAddress("localhost", 1232)
    val a3: InetSocketAddress = InetSocketAddress("localhost", 1233)
    val a4: InetSocketAddress = InetSocketAddress("localhost", 1234)
    val connectionManager = ConnectionManager(a1, a2)
    val chatManager = ChatManager(connectionManager)
    chatManager.createChat(666)

    //connectionManager.close()
}
