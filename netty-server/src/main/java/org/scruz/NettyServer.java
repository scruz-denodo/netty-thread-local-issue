package org.scruz;

import java.util.concurrent.atomic.AtomicInteger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultThreadFactory;

public class NettyServer {

    private final int port;
    private Channel serverChannel;

    public NettyServer(int port) {
        this.port = port;
    }

    public void start() throws InterruptedException {
        final NioEventLoopGroup acceptGroup = new NioEventLoopGroup(2, new MyThreadFactory("Accepter"));
        final NioEventLoopGroup workerGroup = new NioEventLoopGroup(2, new MyThreadFactory("Worker"));

        final ServerBootstrap b = new ServerBootstrap();
        b.group(acceptGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    final ChannelPipeline pipeline = socketChannel.pipeline();
                    pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8))
                        .addLast(new StringEncoder(CharsetUtil.UTF_8))
                        .addLast(new ServerHandler());
                }
            });

        serverChannel = b.bind(port)
            .sync()
            .channel();

        System.out.println("Server listening on: " + serverChannel.localAddress());
        serverChannel.closeFuture().sync();
    }

    static class MyThreadFactory extends DefaultThreadFactory {

        private final AtomicInteger counter = new AtomicInteger(0);

        private final String thPrexix;

        public MyThreadFactory(String poolName) {
            super(poolName);
            this.thPrexix = poolName + '(';
        }

        @Override
        public Thread newThread(Runnable r) {
            final Thread th = super.newThread(r);
            th.setName(thPrexix + counter.getAndIncrement() + ')');
            return th;
        }
    }

    public static void main(String args[]) throws InterruptedException {
        if (args.length != 1) {
            System.err.println("usage -> NettyServer <port>");
            System.exit(1);
        }

        final NettyServer server =  new NettyServer(Integer.parseInt(args[0]));
        server.start();
    }
}
