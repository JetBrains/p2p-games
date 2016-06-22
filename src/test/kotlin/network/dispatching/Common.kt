package network.dispatching

import proto.ChatMessageProto
import proto.GenericMessageProto
import proto.QueryProto
import java.net.InetSocketAddress

/**
 * Created by user on 6/22/16.
 */

fun getSampleChatMessage(): ChatMessageProto.ChatMessage {
    val chatMessage = ChatMessageProto.ChatMessage.newBuilder()
    chatMessage.chatId = 666
    chatMessage.message = "Need more Souls"
    val a2: InetSocketAddress = InetSocketAddress("localhost", 1232)
    chatMessage.user = (ChatMessageProto.User.newBuilder().setHostname(a2.hostName).setPort(a2.port).setName("Mark Geller").build())
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