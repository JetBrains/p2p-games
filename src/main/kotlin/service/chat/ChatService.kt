package service.chat

import GUI.ChatGUI
import broker.NettyGroupBroker
import entity.Group
import entity.User
import network.ConnectionManager
import network.Service
import network.dispatching.Dispatcher
import network.dispatching.SimpleDispatcher
import proto.ChatMessageProto
import proto.GenericMessageProto
import java.net.InetSocketAddress
import javax.swing.UIManager

/**
 * Created by user on 6/22/16.
 * Represents logic behind chat
 */
class ChatService(val chatGUI: ChatGUI, private val connectionManager: ConnectionManager): Service<ChatMessageProto.ChatMessage>, Runnable{
    // chat users in this group
    val group = Group(mutableSetOf())
    val groupBroker = NettyGroupBroker(connectionManager)

    init{
        //TODO remove callbacks
        connectionManager.addService(GenericMessageProto.GenericMessage.Type.CHAT_MESSAGE, this)
        chatGUI.chatService = this
    }

    /**
     * Receive and process general purpose message
     */
    fun receiveMessage(msg: ChatMessageProto.ChatMessage){
        chatGUI.displayMessage(msg.chatId, msg.user.name, msg.message)
        group.users.add(User(InetSocketAddress(msg.user.hostname, msg.user.port), msg.user.name))
    }


    /**
     * Receive and process general purpose message
     * callback from gui on submit button
     */
    fun sendMessage(message: String){
        //TODO separate protobuf factory
        val user = ChatMessageProto.User.newBuilder().setHostname(connectionManager.hostAddr.hostName)
                .setPort(connectionManager.hostAddr.port).setName(chatGUI.username).build()
        val chatMessage = ChatMessageProto.ChatMessage.newBuilder().setChatId(chatGUI.chatID).setMessage(message)
                .setUser(user).build()
        val msg = GenericMessageProto.GenericMessage.newBuilder().setChatMessage(chatMessage)
                .setType(GenericMessageProto.GenericMessage.Type.CHAT_MESSAGE).setChatMessage(chatMessage).build()
        groupBroker.broadcast(group, msg)
    }

    /**
     * protobuf dispatcher
     * @see Dispatcher
     */
    override fun getDispatcher(): Dispatcher<ChatMessageProto.ChatMessage> {
        fun response(msg: ChatMessageProto.ChatMessage): GenericMessageProto.GenericMessage?{
            receiveMessage(msg)
            return null
        }
        return SimpleDispatcher(::response)
    }

    override fun run(){
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        chatGUI.preDisplay()
    }
}