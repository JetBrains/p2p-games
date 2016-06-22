package service

import apps.chat.Chat
import network.Service
import network.dispatching.Dispatcher
import network.dispatching.EnumDispatcher
import proto.GenericMessageProto
import proto.QueryProto

/**
 * Created by user on 6/22/16.
 */

class QueryService(private val chat: Chat): Service<QueryProto.Query>{
    fun queryChatMembers(query: QueryProto.ChatMemberQuery): GenericMessageProto.GenericMessage?{
        return null
    }
    override fun getDispatcher(): Dispatcher<QueryProto.Query> {
        val queryDispatcher = EnumDispatcher(QueryProto.Query.getDefaultInstance())
        val func = {x: QueryProto.ChatMemberQuery -> queryChatMembers(x)}
        queryDispatcher.register(QueryProto.Query.Type.CHAT_MEMBER_QUERY, func)
        return queryDispatcher
    }

}