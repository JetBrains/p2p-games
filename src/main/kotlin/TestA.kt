import apps.chat.ChatManager
import network.ConnectionManager
import org.apache.log4j.BasicConfigurator
import java.net.InetSocketAddress


/**
 * Created by user on 6/20/16.
 */

fun main(args: Array<String>) {
    val chats: Int = 3

    BasicConfigurator.configure();
    for(i in 1..2*chats step 2){
        val a1: InetSocketAddress = InetSocketAddress("localhost", 1230 + i)
        val a2: InetSocketAddress = InetSocketAddress("localhost", 1231 + i)
        ChatManager(ConnectionManager(a1, a2))
    }

    //connectionManager.close()
}
