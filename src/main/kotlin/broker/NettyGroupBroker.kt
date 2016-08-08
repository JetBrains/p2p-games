package broker

import entity.Group
import entity.User
import network.ConnectionManager
import network.ConnectionManagerClass
import proto.GenericMessageProto

/**
 * Created by user on 6/20/16.
 */
class NettyGroupBroker(val connectionManager: ConnectionManagerClass = ConnectionManager) : GroupBroker {
    override fun broadcastAsync(group: Group,
                                msg: GenericMessageProto.GenericMessage) {
        synchronized(group.users) {
            for (user in group.users) {
                sendAsync(user, msg)
            }
        }
    }


    override fun sendAsync(user: User,
                           msg: GenericMessageProto.GenericMessage) {
        connectionManager.sendAsync(user.hostAddress, msg)
    }


}