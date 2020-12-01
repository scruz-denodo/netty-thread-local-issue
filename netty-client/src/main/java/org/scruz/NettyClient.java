package org.scruz;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;

public class NettyClient {

    private final String host;
    private final int port;
    private NioEventLoopGroup accepter;
    private EventExecutorGroup executors;
    private ChannelFuture clientChannel;

    public NettyClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws Exception {
        accepter = new NioEventLoopGroup(2, new MyThreadFactory("Accepter"));
        executors = new DefaultEventExecutorGroup(2, new MyThreadFactory("EvtExecutor"));
        final Bootstrap b = new Bootstrap();
        b.group(accepter)
            .channel(NioSocketChannel.class)
            .handler(new ChannelInitializer<>() {
                @Override
                protected void initChannel(Channel channel) throws Exception {
                    channel.pipeline()
                        .addLast(new StringDecoder(CharsetUtil.UTF_8))
                        .addLast(new StringEncoder(CharsetUtil.UTF_8))
                        .addLast(executors, new ClientHandler());
                }
            });

        System.out.println("Connecting netty client with thread: " + Thread.currentThread().getName());
        clientChannel = b.connect(host, port).sync();
    }

    public void disconnect() throws InterruptedException {
        clientChannel.channel().disconnect();
        if (accepter!=null) {
            accepter.shutdownGracefully();
        }
        if (executors!=null) {
            executors.shutdownGracefully();
        }
    }

    public void sendMessage(String msg) throws Exception {
        connect();
        clientChannel.channel().writeAndFlush(msg).sync();
        disconnect();
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

    public static void main(String args[]) throws Exception {
        if (args.length!=2) {
            System.err.println("usage -> NettyClient <host> <port>");
            System.exit(1);
        }

        final AtomicLong i = new AtomicLong(0);

        final ExecutorService executor = Executors.newFixedThreadPool(2);
        final int NTASKS = 100;
        final AtomicInteger finishedTasks = new AtomicInteger(0);
        final Lock l = new ReentrantLock();
        final Condition finishedExecutions = l.newCondition();

        for (int e=0;e<NTASKS;e++) {
            executor.execute(() -> {
                    try {
                        final NettyClient nettyClient = new NettyClient(args[0], Integer.parseInt(args[1]));
                        nettyClient.sendMessage("My message " + i.incrementAndGet());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    } finally {
                        l.lock();
                        if (finishedTasks.incrementAndGet()==NTASKS) {
                            finishedExecutions.signalAll();
                        }
                        l.unlock();
                    }
                });
        }

        l.lock();
        if (finishedTasks.get()<NTASKS) {
            finishedExecutions.await();
        }

        System.out.println();
    }


}
