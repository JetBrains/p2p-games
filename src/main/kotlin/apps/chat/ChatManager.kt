package apps.chat

import apps.chat.GUI.ChatGUI
import entity.User
import network.ConnectionManager
import network.Service
import network.dispatching.Dispatcher
import network.dispatching.EnumDispatcher
import network.dispatching.SimpleDispatcher
import proto.ChatMessageProto
import proto.EntitiesProto
import proto.GenericMessageProto
import proto.QueryProto
import java.net.InetSocketAddress

/**
 * Created by Mark Geller on 6/23/16.
 * Entity for managing chats
 */

class ChatManager(private val connectionManager: ConnectionManager) {
    val chats = mutableSetOf<Chat>()

    init {
        connectionManager.addService(GenericMessageProto.GenericMessage.Type.QUERY,
                ChatQueryService(this))

        //TODO remove callbacks
        connectionManager.addService(GenericMessageProto.GenericMessage.Type.CHAT_MESSAGE,
                ChatService(this))
    }

    /**
     * Process some incomming message(send it to corresponding chat)
     * @param msg - incomming message
     */
    fun receiveMessage(msg: ChatMessageProto.ChatMessage) {
        val chat = chats.firstOrNull { x -> x.chatId == msg.chatId }
        if (chat == null) {
            createChat(msg.chatId)
        }
        chats.first { x -> x.chatId == msg.chatId }.showMessage(msg)
    }

    /**
     * create chat with given Id(and gui for it)
     * @param msg - Id of chat to be created
     */
    fun createChat(chatId: Int): Chat {
        val chat = Chat(ChatGUI(), connectionManager, chatId)
        Thread(chat).start()
        chats.add(chat)
        return chat
    }

    /**
     * create chat with given Id(and gui for it)
     * @param chatId - id of chat to be created
     */
    fun getOrCreateChat(chatId: Int): Chat {
        var chat = chats.find { x -> x.chatId == chatId }
        if (chat == null) {
            chat = createChat(chatId)
        }
        return chat
    }

    /**
     * Join chat with given chatId(request
     * members for known host)
     * create it if need.
     */
    fun joinChat(chatId: Int, memberAddr: InetSocketAddress) {
        //TODO - query factory
        val query = GenericMessageProto.GenericMessage.newBuilder()
                .setType(GenericMessageProto.GenericMessage.Type.QUERY)
                .setQuery(QueryProto.Query.newBuilder()
                        .setType(QueryProto.Query.Type.CHAT_MEMBER_QUERY)
                        .setQuery(QueryProto.ChatMemberQuery.newBuilder().setChatID(chatId).build())).build()
        val request = connectionManager.request(memberAddr, query)
        val chat = getOrCreateChat(chatId)

        for (user in request.responseGroup.responseList[0].group.usersList) {
            chat.group.users.add(User(InetSocketAddress(user.hostname, user.port), user.name))
        }
    }
}

/**
 * Service for dispatching incoming messages
 */
class ChatService(private val chatManager: ChatManager) : Service<ChatMessageProto.ChatMessage> {

    override fun getDispatcher(): Dispatcher<ChatMessageProto.ChatMessage> {
        fun response(msg: ChatMessageProto.ChatMessage): GenericMessageProto.GenericMessage? {
            chatManager.receiveMessage(msg)
            return null
        }
        return SimpleDispatcher(::response)
    }

}

/**
 * Service for dispatching incoming qyeries
 */
class ChatQueryService(private val manager: ChatManager) : Service<QueryProto.Query> {

    fun queryChatMembers(query: QueryProto.ChatMemberQuery): GenericMessageProto.GenericMessage? {
        val chat = manager.chats.find { x -> x.chatId == query.chatID }
        var response = EntitiesProto.Group.newBuilder().build()
        if (chat != null) {
            response = chat.group.getProto()
        }
        return GenericMessageProto.GenericMessage.newBuilder()
                .setType(GenericMessageProto.GenericMessage.Type.GROUP)
                .setGroup(response).build()
    }


    override fun getDispatcher(): Dispatcher<QueryProto.Query> {
        val queryDispatcher = EnumDispatcher(QueryProto.Query.getDefaultInstance())
        val func = { x: QueryProto.ChatMemberQuery -> queryChatMembers(x) }
        queryDispatcher.register(QueryProto.Query.Type.CHAT_MEMBER_QUERY, func)
        return queryDispatcher
    }
}