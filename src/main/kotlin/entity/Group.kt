package entity

/**
 * Created by user on 6/20/16.
 */

class Group(users: MutableSet<User>){
    val users: MutableSet<User> = users

    constructor() : this(mutableSetOf()) {}
}