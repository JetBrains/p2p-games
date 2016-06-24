/**
 * Created by user on 6/24/16.
 */
import apps.chat.ChatManager
import apps.games.GameManager
import network.ConnectionManager
import org.apache.log4j.BasicConfigurator
import java.net.InetSocketAddress


/**
 * Created by user on 6/20/16.
 */

fun main(args: Array<String>) {
    BasicConfigurator.configure();
    val a1: InetSocketAddress = InetSocketAddress("localhost", 1235)
    val a2: InetSocketAddress = InetSocketAddress("localhost", 1236)
    Settings.clientAddress = a1
    Settings.hostAddress = a2
    ChatManager.start()
    GameManager.start()

    //connectionManager.close()
}
