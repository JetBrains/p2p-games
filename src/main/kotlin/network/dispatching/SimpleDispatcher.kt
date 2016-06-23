package network.dispatching

import com.google.protobuf.GeneratedMessage
import proto.GenericMessageProto

/**
 * Created by user on 6/22/16.
 */


fun <T : GeneratedMessage> SimpleDispatcher(handler: (T) -> GenericMessageProto.GenericMessage?): Dispatcher<T> = object : Dispatcher<T> {
    override fun dispatch(message: T): GenericMessageProto.GenericMessage? {
        return handler(message)
    }
}