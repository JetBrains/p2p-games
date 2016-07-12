import java.net.InetSocketAddress

/**
 * Created by user on 6/24/16.
 */

//TODO - better pattern
val DEBUG = false

object Settings {
    var clientAddress: InetSocketAddress = InetSocketAddress(1231)
    var hostAddress: InetSocketAddress = InetSocketAddress(1232)
    var defaultUsername = "Unknown"
}
