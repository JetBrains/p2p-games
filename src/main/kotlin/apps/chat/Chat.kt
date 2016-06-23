package apps.chat

import apps.chat.GUI.ChatGUI
import broker.NettyGroupBroker
import entity.Group
import entity.User
import network.ConnectionManager
import proto.ChatMessageProto
import proto.EntitiesProto
import proto.GenericMessageProto
import java.net.InetSocketAddress
import javax.swing.UIManager

/**
 * Created by user on 6/22/16.
 * Represents logic behind chat
 */
class Chat(val chatGUI: ChatGUI, private val connectionManager: ConnectionManager, val chatId: Int) : Runnable {
    // chat users in this group
    val group = Group(mutableSetOf())
    val groupBroker = NettyGroupBroker(connectionManager)
    val username: String = "Unknown"

    init {

        chatGUI.chat = this
    }

    /**
     * Receive and process general purpose message
     */
    fun showMessage(msg: ChatMessageProto.ChatMessage) {
        chatGUI.displayMessage(msg.chatId, msg.user.name, msg.message)
    }

    /**
     * register new chat member
     */
    fun addMember(user: User){
        group.users.add(user)
    }

    /**
     * Compose general purpose message
     * callback from gui on submit button
     */
    fun sendMessage(message: String) {
        //TODO separate protobuf factory
        val user = EntitiesProto.User.newBuilder().setHostname(connectionManager.hostAddr.hostName)
                .setPort(connectionManager.hostAddr.port)
                .setName(chatGUI.username).build()

        val chatMessage = ChatMessageProto.ChatMessage.newBuilder()
                .setChatId(chatId)
                .setMessage(message)
                .setUser(user).build()

        val msg = GenericMessageProto.GenericMessage.newBuilder()
                .setChatMessage(chatMessage)
                .setType(GenericMessageProto.GenericMessage.Type.CHAT_MESSAGE).build()
        groupBroker.broadcast(group, msg)
    }

    //todo
    /**
     * self registration
     */
    fun register(username: String) {
        group.users.add(User(connectionManager.hostAddr, username))
    }

    override fun run() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        chatGUI.preDisplay()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Chat

        if (chatId != other.chatId) return false

        return true
    }

    override fun hashCode(): Int {
        return chatId
    }

    //TODO - only close connectons of this chat
    fun close(){
        connectionManager.close()
    }
}