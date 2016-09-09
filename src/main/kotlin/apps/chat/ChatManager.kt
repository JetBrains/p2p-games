package apps.chat

import apps.chat.gui.ChatManagerGUI
import apps.games.GameManager
import entity.ChatMessage
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
import javax.swing.UIManager

/**
 * Created by Mark Geller on 6/23/16.
 * Entity for managing chats
 */

object ChatManager {
    val chats = mutableSetOf<Chat>()
    internal val mainGUI = ChatManagerGUI()
    fun start() {
        ConnectionManager.addService(
                GenericMessageProto.GenericMessage.Type.QUERY,
                ChatQueryService(this))

        ConnectionManager.addService(
                GenericMessageProto.GenericMessage.Type.CHAT_MESSAGE,
                ChatService(this))


        //Init playground.main gui
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        mainGUI.display()
    }

    /**
     * Process some incomming message(send it to corresponding chat)
     * @param msg - incomming message
     */
    fun receiveMessage(msg: ChatMessageProto.ChatMessage) {
        val chat = getOrCreateChat(msg.chatId)
        chat.showMessage(ChatMessage(msg))
        chat.addMember(User(InetSocketAddress(msg.user.hostname, msg.user.port),
                msg.user.name))
    }

    /**
     * create chat with given Id(and gui for it)
     * @param msg - Id of chat to be created
     */
    @Synchronized fun createChat(chatId: Int): Chat {
        val chat = Chat(chatId)
        Thread(chat).start()
        chats.add(chat)
        return chat
    }

    @Synchronized fun getChatOrNull(chatId: Int): Chat? {
        return chats.find { x -> x.chatId == chatId }
    }

    /**
     * create chat with given Id(and gui for it)
     * @param chatId - id of chat to be created
     */
    @Synchronized fun getOrCreateChat(chatId: Int): Chat {
        var chat = getChatOrNull(chatId)
        if (chat == null) {
            chat = createChat(chatId)
        }
        if (chat.chatGUI.isClosed) {
            chat.chatGUI.reopen()
        }
        return chat
    }

    /**
     * Join chat with given chatId(request
     * members for known host)
     * create it if need.
     */
    @Synchronized fun joinChat(chatId: Int,
                               memberAddr: InetSocketAddress,
                               username: String): Chat {
        //TODO - query factory
        val query = GenericMessageProto.GenericMessage.newBuilder()
                .setType(GenericMessageProto.GenericMessage.Type.QUERY)
                .setQuery(QueryProto.Query.newBuilder()
                        .setType(QueryProto.Query.Type.CHAT_MEMBER_QUERY)
                        .setChatMemberQuery(
                                QueryProto.ChatMemberQuery.newBuilder().setChatID(
                                        chatId).build())).build()
        val request = ConnectionManager.request(memberAddr, query)
        val chat = getOrCreateChat(chatId)
        chat.register(username)
        for (user in request.responseGroup.responseList[0].group.usersList) {
            chat.addMember(User(InetSocketAddress(user.hostname, user.port),
                    user.name))
        }
        chat.sendMessage("${chat.username} joined chat #${chat.chatId}!")
        return chat
    }

    /**
     * shutdown all connections
     */
    @Synchronized fun close() {
        ConnectionManager.close()
        GameManager.close()
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
        val queryDispatcher = EnumDispatcher(
                QueryProto.Query.getDefaultInstance())
        val func = { x: QueryProto.ChatMemberQuery -> queryChatMembers(x) }
        queryDispatcher.register(QueryProto.Query.Type.CHAT_MEMBER_QUERY, func)
        return queryDispatcher
    }
}