package broker

import entity.Group
import entity.User
import network.ConnectionManager
import proto.GenericMessageProto

/**
 * Created by user on 6/20/16.
 */
class NettyGroupBroker() : GroupBroker {
    override fun broadcastAsync(group: Group, msg: GenericMessageProto.GenericMessage) {
        for (user in group.users) {
            sendAsync(user, msg)
        }
    }


    override fun sendAsync(user: User, msg: GenericMessageProto.GenericMessage) {
        ConnectionManager.sendAsync(user.hostAddress, msg)
    }


}