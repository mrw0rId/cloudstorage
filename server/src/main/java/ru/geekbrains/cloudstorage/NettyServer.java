package ru.geekbrains.cloudstorage;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import ru.geekbrains.cloudstorage.auth.AuthService;
import ru.geekbrains.cloudstorage.auth.BasicAuthService;
import ru.geekbrains.cloudstorage.handlers.ClientCommandsHandler;

import java.net.SocketException;

public class NettyServer {

    private final int inetPort = 8089;
    private AuthService authService;

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        authService = new BasicAuthService();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new ClientCommandsHandler(authService));
                        }
                    });
            ChannelFuture f = b.bind(inetPort).sync();
            System.out.println("Server started");
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        new NettyServer().run();
    }
}
