package entity

import proto.EntitiesProto
import java.net.InetSocketAddress

/**
 * Created by Mark Geller on 6/20/16.
 */

class User(val hostAddress: InetSocketAddress, val name: String) {



    fun getProto(): EntitiesProto.User {
        return EntitiesProto.User.newBuilder()
                .setHostname(hostAddress.hostName)
                .setPort(hostAddress.port)
                .setName(name).build()
    }

    override fun equals(other: Any?): Boolean{
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as User

        if (hostAddress != other.hostAddress) return false

        return true
    }

    override fun hashCode(): Int{
        return hostAddress.hashCode()
    }
}