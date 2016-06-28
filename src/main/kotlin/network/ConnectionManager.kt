package network

import network.dispatching.EnumDispatcher
import proto.GenericMessageProto
import java.net.InetSocketAddress

/**
 * Created by user on 6/22/16.
 */
object ConnectionManager{
    val dispatcher = EnumDispatcher(GenericMessageProto.GenericMessage.getDefaultInstance())

    private val client = MessageClient(Settings.clientAddress)
    private val server = MessageServer(Settings.hostAddress, dispatcher)

    val services = mutableSetOf<Service<*>>()

    fun addService(event: GenericMessageProto.GenericMessage.Type, service: Service<*>) {
        dispatcher.register(event, service.getDispatcher())
        services.add(service)
    }

    fun sendAsync(addr: InetSocketAddress, msg: GenericMessageProto.GenericMessage) {
        client.sendAsync(addr, msg)
    }

    fun request(addr: InetSocketAddress, msg: GenericMessageProto.GenericMessage): GenericMessageProto.GenericMessage {
        return client.request(addr, msg)
    }

    fun close() {
        client.close()
        server.close()
    }
}