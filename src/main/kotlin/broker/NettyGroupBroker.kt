package broker

import entity.Group
import entity.User

/**
 * Created by user on 6/20/16.
 */
class NettyGroupBroker : GroupBroker{

    override fun Broadcast(group: Group) {
        for(user in group.users){
            send(user)
        }
    }

    override fun send(user: User) {
        throw UnsupportedOperationException()
    }
}