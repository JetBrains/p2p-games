package entity

import proto.EntitiesProto
import java.net.InetSocketAddress

/**
 * Created by Mark Geller on 6/20/16.
 */

class User(val hostAddress: InetSocketAddress, val name: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as User

        if (hostAddress != other.hostAddress) return false
        if (name != other.name) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hostAddress.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    fun getProto(): EntitiesProto.User {
        return EntitiesProto.User.newBuilder()
                .setHostname(hostAddress.hostName)
                .setPort(hostAddress.port)
                .setName(name).build()
    }
}