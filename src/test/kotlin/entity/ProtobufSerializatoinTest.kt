package entity

import common.getSampleUser1
import common.getSmapleGroup
import org.junit.Assert.*
import org.junit.Test
import proto.EntitiesProto

/**
 * Created by user on 6/24/16.
 */
class ProtobufSerializatoinTest{
    @Test
    fun testUserSerialization(){
        val userProto: EntitiesProto.User = getSampleUser1()
        val user = User(userProto)
        assertEquals(user.hostAddress.hostName, userProto.hostname)
        assertEquals(user.hostAddress.port, userProto.port)
        assertEquals(user.name, userProto.name)
        val userProto2 = user.getProto()
        assertEquals(user.hostAddress.hostName, userProto2.hostname)
        assertEquals(user.hostAddress.port, userProto2.port)
        assertEquals(user.name, userProto2.name)
    }

    @Test
    fun testGroupSerialization(){
        val groupProto: EntitiesProto.Group = getSmapleGroup()
        val group = Group(groupProto)
        assertEquals(group.users.size, groupProto.usersList.size)
        assertEquals(group.users.first().name, groupProto.usersList.first().name)
        val groupProto2 = group.getProto()
        assertEquals(group.users.size, groupProto2.usersList.size)
        assertEquals(group.users.first().name, groupProto2.usersList.first().name)
    }
}