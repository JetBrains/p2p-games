package network

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.protobuf.ProtobufDecoder
import io.netty.handler.codec.protobuf.ProtobufEncoder
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender
import io.netty.util.concurrent.Future
import io.netty.util.concurrent.FutureListener
import proto.GameMessageProto
import proto.GenericMessageProto
import java.net.InetSocketAddress
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Created by Mark Geller on 6/21/16.
 * simple message client - transfers protobuf messages to server
 */
class MessageClient(val addr: InetSocketAddress) {
    //Upstream connections
    private val bootstrap = Bootstrap()
    private val connections: MutableMap<InetSocketAddress, ChannelFuture> = mutableMapOf()

    init {
        val group = NioEventLoopGroup()
        bootstrap.group(group).channel(NioSocketChannel::class.java).
                handler(MessageClientChannelInitializer())
        bootstrap.option(ChannelOption.SO_REUSEADDR, true)
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true)
    }


    /**
     * Send Protobuff message to Sever(async),
     * no response required
     * @param host - Server to receive message
     * @param msg - Protobuff message
     */
    fun sendAsync(host: InetSocketAddress, msg: GenericMessageProto.GenericMessage) {
        var f = connections[host]
        if(f == null || !f.channel().isOpen){
            f = bootstrap.connect(host, addr).await().sync() ?: return
            connections[host] = f
        }
        f.channel().writeAndFlush(msg)
    }

    /**
     * Send Protobuff message to Sever(async),
     * and wait for response
     * @param host - Server to receive message
     * @param msg - Protobuff message
     */
    fun request(host: InetSocketAddress, msg: GenericMessageProto.GenericMessage): GenericMessageProto.GenericMessage {
        var f = connections[host]
        if(f == null || !f.channel().isOpen){
            f = bootstrap.connect(host, addr).await().sync() ?: throw Exception("Something went wrong")
            connections[host] = f
        }
        val handler = f.channel().pipeline().get(MessageClientHandler::class.java)
        val response: GenericMessageProto.GenericMessage = handler.request(msg)
        return response
    }

    /**
     * Terminate connection
     */
    fun close() {
        bootstrap.group().shutdownGracefully()
        for(channelFuture in connections.values){
            channelFuture.channel().close().sync()
        }
    }
}


/**
 * Client response handler (process message from server)
 */
class MessageClientHandler : SimpleChannelInboundHandler<GenericMessageProto.GenericMessage>(false) {
    internal val responses = LinkedBlockingQueue<GenericMessageProto.GenericMessage>()
    @Volatile internal var channel: Channel? = null

    fun request(msg: GenericMessageProto.GenericMessage): GenericMessageProto.GenericMessage {
        channel!!.writeAndFlush(msg)
        var interrupted = false
        val response: GenericMessageProto.GenericMessage?
        while (true) {
            try {
                response = responses.take()
                break
            } catch(ignored: InterruptedException) {
                interrupted = true
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt()
        }
        return response?: GenericMessageProto.GenericMessage.getDefaultInstance()
    }

    override fun channelRegistered(ctx: ChannelHandlerContext?) {
        this.channel = ctx!!.channel()
        //super.channelRegistered(ctx)
    }

    override fun channelRead0(ctx: ChannelHandlerContext?, msg: GenericMessageProto.GenericMessage?) {
        if (msg != null) {
            responses.add(msg)
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}


/**
 * Pipeline for protobuf network serialization/deserialization
 */
class MessageClientChannelInitializer : ChannelInitializer<SocketChannel> () {
    override fun initChannel(ch: SocketChannel?) {
        val pipeline = ch!!.pipeline()
        pipeline.addLast(ProtobufVarint32FrameDecoder())
        pipeline.addLast(ProtobufDecoder(GenericMessageProto.GenericMessage.getDefaultInstance()))
        pipeline.addLast(ProtobufVarint32LengthFieldPrepender())
        pipeline.addLast(ProtobufEncoder())
        pipeline.addLast(MessageClientHandler())
    }

}
