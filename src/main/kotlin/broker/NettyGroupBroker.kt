package broker

import network.MessageClient
import network.MessageServer
import entity.Group
import entity.User
import network.ConnectionManager
import proto.GenericMessageProto
import java.net.InetSocketAddress

/**
 * Created by user on 6/20/16.
 */
class NettyGroupBroker(val connectionManager: ConnectionManager): GroupBroker{


    override fun broadcast(group: Group, msg: GenericMessageProto.GenericMessage) {
        for(user in group.users){
            send(user, msg)
        }
    }

    override fun send(user: User, msg: GenericMessageProto.GenericMessage) {
        connectionManager.send(user.hostAddress, msg)
    }
}