package ru.geekbrains.cloudstorage;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import ru.geekbrains.cloudstorage.handlers.ServerAnswerHandler;
import ru.geekbrains.cloudstorage.util.GUI;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

public class Network {

    private final int inetPort = 8089;
    private final String host = "localhost";
    private Channel currentChannel;
    private GUI gui;
    private NettyClient nettyClient;

    public Channel getCurrentChannel() {
        return currentChannel;
    }

    public void start(CountDownLatch countDownLatch, GUI gui, NettyClient nettyClient) {
        EventLoopGroup group = new NioEventLoopGroup();
        this.gui = gui;
        this.nettyClient = nettyClient;
        try {
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(host, inetPort))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline()
                                    .addLast(new ServerAnswerHandler(gui, nettyClient));
                            currentChannel = socketChannel;
                        }
                    });
            ChannelFuture channelFuture = clientBootstrap.connect().sync();
            System.out.println("Client connected");
            countDownLatch.countDown();
            channelFuture.channel().closeFuture().sync();
        } catch (Exception e) {
            System.out.println("Server is not available");
            e.printStackTrace();
        } finally {
            try {
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        currentChannel.close();
    }
}
