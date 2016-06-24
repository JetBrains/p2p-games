import proto.ChatMessageProto
import proto.EntitiesProto
import proto.GenericMessageProto
import proto.QueryProto
import java.net.InetSocketAddress

/**
 * Created by user on 6/22/16.
 */

fun getSampleUser1(): EntitiesProto.User {
    val addr: InetSocketAddress = InetSocketAddress("localhost", 1231)
    return EntitiesProto.User.newBuilder().setHostname(addr.hostName).setPort(addr.port).setName("Mark Geller").build()
}

fun getSampleUser2(): EntitiesProto.User {
    val addr: InetSocketAddress = InetSocketAddress("localhost", 1232)
    return EntitiesProto.User.newBuilder().setHostname(addr.hostName).setPort(addr.port).setName("Mark Geller").build()
}

fun getSampleUser3(): EntitiesProto.User {
    val addr: InetSocketAddress = InetSocketAddress("localhost", 1233)
    return EntitiesProto.User.newBuilder().setHostname(addr.hostName).setPort(addr.port).setName("Mark Geller").build()
}

fun getSmapleGroup(): EntitiesProto.Group {
    return EntitiesProto.Group.newBuilder()
            .addUsers(getSampleUser1())
            .addUsers(getSampleUser2())
            .addUsers(getSampleUser3()).build()
}

fun getSampleChatMessage(): ChatMessageProto.ChatMessage {
    val chatMessage = ChatMessageProto.ChatMessage.newBuilder()
    chatMessage.chatId = 666
    chatMessage.message = "Need more Souls"

    chatMessage.user = getSampleUser3()
    return chatMessage.build()
}

fun getSampleGenericMessage(): GenericMessageProto.GenericMessage {
    val genericMessage = GenericMessageProto.GenericMessage.newBuilder()
            .setType(GenericMessageProto.GenericMessage.Type.CHAT_MESSAGE).setChatMessage(getSampleChatMessage()).build()
    return genericMessage
}

fun getSampleQuery(): QueryProto.Query {
    val chatQuery = QueryProto.ChatMemberQuery.newBuilder().setChatID(666).build()
    val query = QueryProto.Query.newBuilder().setQuery(chatQuery).setType(QueryProto.Query.Type.CHAT_MEMBER_QUERY).build()
    return query
}

fun getSampleGenericQuery(): GenericMessageProto.GenericMessage {
    val genericMessage = GenericMessageProto.GenericMessage.newBuilder()
            .setType(GenericMessageProto.GenericMessage.Type.QUERY).setQuery(getSampleQuery()).build()
    return genericMessage
}