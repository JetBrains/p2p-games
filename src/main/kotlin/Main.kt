/**
 * Created by user on 8/12/16.
 */
import apps.chat.ChatManager
import apps.games.GameManager
import org.apache.commons.cli.*
import org.apache.log4j.BasicConfigurator
import org.lwjgl.Sys
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Created by user on 6/20/16.
 */

fun main(args: Array<String>) {
    val cmd: CommandLine
    OptionBuilder.withArgName("client")
    OptionBuilder.hasArg()
    OptionBuilder.withType(Int)
    OptionBuilder.withDescription("client port")
    val clientOption: Option = OptionBuilder.create("c")

    OptionBuilder.withArgName("server")
    OptionBuilder.hasArg()
    OptionBuilder.withType(Int)
    OptionBuilder.withDescription("server port")
    val serverOption: Option = OptionBuilder.create("s")

    OptionBuilder.withArgName("name")
    OptionBuilder.hasArg()
    OptionBuilder.withDescription("username")
    val usernameOption: Option = OptionBuilder.create("u")

    val helpOption = Option("help", "show help")

    val options = Options()
    options.addOption(clientOption)
    options.addOption(serverOption)
    options.addOption(usernameOption)
    options.addOption(helpOption)
    val parser = GnuParser()
    var clientPort: Int = 1231
    var serverPort: Int = 1232
    var username: String = "Alice"
    try{
        cmd = parser.parse(options, args)
        if(cmd.hasOption("c")){
            clientPort = cmd.getOptionValue("c").toInt()
        }

        if(cmd.hasOption("s")){
            serverPort = cmd.getOptionValue("s").toInt()
        }

        if(cmd.hasOption("u")){
            username = cmd.getOptionValue("u")
        }
        if(cmd.hasOption("help")){
            System.out.println("Available parameters: \n" +
                                "-c=PORT -- specify client port to be at PORT\n" +
                                "-s=PORT -- specify server port to be at PORT\n" +
                                "-u=NAME -- set default username")
            System.exit(0)
        }
    } catch (e: ParseException){
        System.err.print("Invalid options")
        System.exit(0)
    }

    BasicConfigurator.configure()
    val host: String
    try {
        val s = Socket("google.com", 80)
        host = s.localAddress.hostAddress
        s.close()
    } catch(ignored: Exception) {
        host = InetAddress.getLocalHost().hostAddress
    }
    val a1: InetSocketAddress = InetSocketAddress(host, clientPort)
    val a2: InetSocketAddress = InetSocketAddress(host, serverPort)
    Settings.clientAddress = a1
    Settings.hostAddress = a2
    Settings.defaultUsername = username
    ChatManager.start()
    GameManager.start()
    //connectionManager.close()
}
