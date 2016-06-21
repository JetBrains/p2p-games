package broker

import entity.Group
import entity.User

/**
 * Created by user on 6/20/16.
 */
interface GroupBroker {
    fun Broadcast(group: Group)

    fun send(user: User)
}