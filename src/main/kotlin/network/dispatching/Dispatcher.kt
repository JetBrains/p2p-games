package network.dispatching

import com.google.protobuf.GeneratedMessage
import proto.GenericMessageProto

/**
 * Created by user on 6/21/16.
 */

@FunctionalInterface
interface Dispatcher<T : GeneratedMessage> {
    fun dispatch(message: T): GenericMessageProto.GenericMessage?
}

