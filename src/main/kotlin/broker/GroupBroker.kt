package broker

import entity.Group
import entity.User
import proto.GenericMessageProto

/**
 * Created by user on 6/20/16.
 */
interface GroupBroker {
    fun broadcast(group: Group, msg: GenericMessageProto.GenericMessage)

    fun send(user: User, msg: GenericMessageProto.GenericMessage)
}