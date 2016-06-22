package service

import network.Service
import network.dispatching.Dispatcher
import network.dispatching.SimpleDispatcher
import proto.ChatMessageProto
import proto.GenericMessageProto

/**
 * Created by user on 6/22/16.
 */

class ChatService(private val callbackGUI: (ChatMessageProto.ChatMessage) -> (Unit)): Service<ChatMessageProto.ChatMessage>{

    override fun getDispatcher(): Dispatcher<ChatMessageProto.ChatMessage> {
        fun response(msg: ChatMessageProto.ChatMessage): GenericMessageProto.GenericMessage?{
            callbackGUI(msg)
            return null
        }
        return SimpleDispatcher(::response)
    }

}