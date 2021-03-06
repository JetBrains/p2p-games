package playground

/**
 * Created by user on 6/24/16.
 */
import DEBUG
import apps.chat.ChatManager
import apps.games.GameManager
import org.apache.log4j.BasicConfigurator
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket


/**
 * Created by user on 6/20/16.
 */

fun main(args: Array<String>) {
    BasicConfigurator.configure()
    val host: String
    if (DEBUG) {
        host = "127.0.0.1"
    } else {
        try {
            val s = Socket("google.com", 80)
            host = s.localAddress.hostAddress
            s.close()
        } catch(ignored: Exception) {
            host = InetAddress.getLocalHost().hostAddress
        }
    }
    val a1: InetSocketAddress = InetSocketAddress(host, 1237)
    val a2: InetSocketAddress = InetSocketAddress(host, 1238)
    Settings.clientAddress = a1
    Settings.hostAddress = a2
    Settings.defaultUsername = "Dave"
    ChatManager.start()
    GameManager.start()

    //connectionManager.close()
}
