package network.dispatching

import common.getSampleChatMessage
import common.getSampleGenericMessage
import common.getSampleGenericQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import proto.ChatMessageProto
import proto.GenericMessageProto
import proto.QueryProto

/**
 * Created by user on 6/22/16.
 */

class DispatcherTest {
    @Test
    fun testSimpleDispatcher() {
        var state = 1
        fun foo(m: ChatMessageProto.ChatMessage): GenericMessageProto.GenericMessage? {
            assertTrue(m.message.equals("Need more Souls"))
            state = 0
            return null
        }

        val dispatcher = SimpleDispatcher(::foo)
        dispatcher.dispatch(getSampleChatMessage())
        assertEquals(state, 0)
    }

    @Test
    fun testEnumDispatcher() {
        var state = 1
        fun foo(m: ChatMessageProto.ChatMessage): GenericMessageProto.GenericMessage? {
            assertTrue(m.message.equals("Need more Souls"))
            state = 0
            return null
        }

        val dispatcher = EnumDispatcher(GenericMessageProto.GenericMessage.getDefaultInstance())
        dispatcher.register(GenericMessageProto.GenericMessage.Type.CHAT_MESSAGE, ::foo)
        dispatcher.dispatch(getSampleGenericMessage())
        assertEquals(state, 0)
    }

    @Test
    fun testNestedDispatcher() {
        var state = 1
        fun foo(m: QueryProto.ChatMemberQuery): GenericMessageProto.GenericMessage? {
            assertEquals(m.chatID, 666)
            state = 0
            return null
        }

        val endpoint = SimpleDispatcher(::foo)
        val queryDispatcher = EnumDispatcher(QueryProto.Query.getDefaultInstance())
        queryDispatcher.register(QueryProto.Query.Type.CHAT_MEMBER_QUERY, endpoint)
        val messageDispatcher = EnumDispatcher(GenericMessageProto.GenericMessage.getDefaultInstance())
        messageDispatcher.register(GenericMessageProto.GenericMessage.Type.QUERY, queryDispatcher)
        messageDispatcher.dispatch(getSampleGenericQuery())
        assertEquals(state, 0)
    }

}