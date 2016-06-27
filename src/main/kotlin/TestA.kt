import apps.chat.ChatManager
import apps.games.Game
import apps.games.GameManager
import network.ConnectionManager
import org.apache.log4j.BasicConfigurator
import java.net.InetSocketAddress


/**
 * Created by user on 6/20/16.
 */

fun main(args: Array<String>) {
    BasicConfigurator.configure();
    val host: String

    if(DEBUG){
        host = "127.0.0.1"
    }else{
        host = "0.0.0.0"
    }

    val a1: InetSocketAddress = InetSocketAddress(host, 1231)
    val a2: InetSocketAddress = InetSocketAddress(host, 1232)
    Settings.clientAddress = a1
    Settings.hostAddress = a2
    ChatManager.start()
    GameManager.start()
    //connectionManager.close()
}
