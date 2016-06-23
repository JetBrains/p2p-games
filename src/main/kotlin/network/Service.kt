package network

import com.google.protobuf.GeneratedMessage
import network.dispatching.Dispatcher

/**
 * Created by user on 6/22/16.
 * Interface describes GenericMessage consumers
 */
interface Service<T : GeneratedMessage> {
    fun getDispatcher(): Dispatcher<T>
}