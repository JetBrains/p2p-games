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
import proto.GenericMessageProto
import java.net.InetSocketAddress

/**
 * Created by Mark Geller on 6/21/16.
 * simple message client - transfers protobuf messages to server
 */
class MessageClient(val addr: InetSocketAddress) {
    //Upstream connections
    private val bootstrap = Bootstrap()

    init{
        val group = NioEventLoopGroup();
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
    fun send(host: InetSocketAddress, msg: GenericMessageProto.GenericMessage) {
        val f = bootstrap.connect(host, addr).sync()
        f.channel().writeAndFlush(msg)
        //TODO reuse channels, not reopen them
        f.channel().close().sync()
    }

    /**
     * Terminate connection
     */
    fun close(){
        bootstrap.group().shutdownGracefully()
    }
}


/**
 * Client response handler (process message from server)
 */
class MessageClientHandler : SimpleChannelInboundHandler<GenericMessageProto.GenericMessage>() {
    override fun channelRead0(ctx: ChannelHandlerContext?, msg: GenericMessageProto.GenericMessage?) {
        print(msg)
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        ctx.close()
    }
}


/**
 * Pipeline for protobuf network serialization/deserialization
 */
class MessageClientChannelInitializer : ChannelInitializer<SocketChannel> (){
    override fun initChannel(ch: SocketChannel?) {
        val pipeline = ch!!.pipeline()
        pipeline.addLast(ProtobufVarint32FrameDecoder())
        pipeline.addLast(ProtobufDecoder(GenericMessageProto.GenericMessage.getDefaultInstance()))
        pipeline.addLast(ProtobufVarint32LengthFieldPrepender())
        pipeline.addLast(ProtobufEncoder())
        pipeline.addLast(MessageClientHandler())
    }

}