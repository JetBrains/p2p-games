package apps.chat

import apps.chat.GUI.ChatGUI
import broker.NettyGroupBroker
import entity.ChatMessage
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
class Chat(val chatId: Int) : Runnable {
    // chat users in this group
    val group = Group(mutableSetOf())
    val groupBroker = NettyGroupBroker()
    var username: String = "Unknown"
    val chatGUI = ChatGUI(this)
    var finatilzed = false

    /**
     * Receive and process general purpose message
     */
    fun showMessage(msg: ChatMessage) {
        if(msg.chatId == chatId){
            chatGUI.displayMessage(msg.user.name, msg.message)
        }
    }

    /**
     * register new chat member
     */
    fun addMember(user: User){
        if(!finatilzed){
            group.users.add(user)
        }
    }

    /**
     * Compose general purpose message
     * callback from gui on submit button
     */
    fun sendMessage(message: String) {
        //TODO separate protobuf factory
        val user = User(Settings.hostAddress, username)

        val chatMessage = ChatMessage(chatId, user, message)

        val msg = GenericMessageProto.GenericMessage.newBuilder()
                .setChatMessage(chatMessage.getProto())
                .setType(GenericMessageProto.GenericMessage.Type.CHAT_MESSAGE).build()
        groupBroker.broadcastAsync(group, msg)
    }

    //todo
    /**
     * self registration
     */
    fun register(username: String) {
        this.username = username
        group.users.add(User(Settings.hostAddress, username))
        chatGUI.refreshTitle("$username[${Settings.hostAddress.toString()}]Chat #$chatId")
    }

    override fun run() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        chatGUI.display()
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

    //TODO - close connectons of this chat
    fun close(){
        println("Chat closing")
    }
}