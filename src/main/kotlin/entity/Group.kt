package entity

import proto.EntitiesProto

/**
 * Created by user on 6/20/16.
 * * Open - for mockito testing purposes
 */

open class Group(users: MutableSet<User>) : ProtobufSerializable<EntitiesProto.Group>, Cloneable {
    open val users: MutableSet<User> = users

    constructor() : this(mutableSetOf()) {
    }

    constructor(group: EntitiesProto.Group) : this(mutableSetOf()) {
        for (user in group.usersList) {
            users.add(User(user))
        }
    }

    override fun getProto(): EntitiesProto.Group {
        val builder = EntitiesProto.Group.newBuilder()
        for (user in users) {
            builder.addUsers(user.getProto())
        }
        return builder.build()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Group

        if (users != other.users) return false

        return true
    }

    override fun hashCode(): Int {
        return users.hashCode()
    }

    override public fun clone(): Group {
        return Group(getProto())
    }
}