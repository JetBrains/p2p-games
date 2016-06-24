package entity

import proto.EntitiesProto

/**
 * Created by user on 6/20/16.
 */

class Group(users: MutableSet<User>) : ProtobufSerializable<EntitiesProto.Group>{
    val users: MutableSet<User> = users

    constructor() : this(mutableSetOf()) {
    }

    override fun getProto(): EntitiesProto.Group {
        val builder = EntitiesProto.Group.newBuilder()
        for (user in users) {
            builder.addUsers(user.getProto())
        }
        return builder.build()
    }
}