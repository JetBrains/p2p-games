import apps.chat.ChatManager
import apps.games.GameManager
import org.apache.log4j.BasicConfigurator
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Created by user on 7/7/16.
 */


class Task: Runnable{
    override fun run() {
        BasicConfigurator.configure();
        val host: String
        if(DEBUG){
            host = "127.0.0.1"
        }else{
            try{
                val s = Socket("google.com", 80)
                host = s.localAddress.hostAddress
                s.close();
            }catch(ignored: Exception){
                host = InetAddress.getLocalHost().hostAddress
            }
        }

        val a1: InetSocketAddress = InetSocketAddress(host, 1233)
        val a2: InetSocketAddress = InetSocketAddress(host, 1234)
        Settings.clientAddress = a1
        Settings.hostAddress = a2
        Settings.defaultUsername = "Bob"
        ChatManager.start()
        GameManager.start()
    }
}

class Task2: Runnable{
    override fun run() {
        BasicConfigurator.configure()
        val host: String
        if(DEBUG){
            host = "127.0.0.1"
        }else{
            try{
                val s = Socket("google.com", 80)
                host = s.localAddress.hostAddress
                s.close();
            }catch(ignored: Exception){
                host = InetAddress.getLocalHost().hostAddress
            }
        }

        val a1: InetSocketAddress = InetSocketAddress(host, 1231)
        val a2: InetSocketAddress = InetSocketAddress(host, 1232)
        Settings.clientAddress = a1
        Settings.hostAddress = a2
        Settings.defaultUsername = "Bob"
        ChatManager.start()
        GameManager.start()
    }
}

fun main(args: Array<String>) {
    Thread(Task()).contextClassLoader
    Thread(Task2()).start()
}