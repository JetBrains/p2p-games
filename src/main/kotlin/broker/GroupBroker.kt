package broker

import entity.Group
import entity.User
import proto.GenericMessageProto

/**
 * Created by user on 6/20/16.
 */
interface GroupBroker {
    fun broadcastAsync(group: Group, msg: GenericMessageProto.GenericMessage)

    fun sendAsync(user: User, msg: GenericMessageProto.GenericMessage)

}