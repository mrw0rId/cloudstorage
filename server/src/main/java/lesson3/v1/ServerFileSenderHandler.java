package lesson3.v1;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ServerFileSenderHandler extends ChannelOutboundHandlerAdapter {

    private final Path rootPath = Paths.get("server" + File.separator + "root_folder");
    private String cmd;
    private String[] splitedCmd;

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        StringBuilder sb = new StringBuilder();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        cmd = sb.append(new String(bytes)).toString();
        splitedCmd = cmd.split(" ");


        ServerFileSender.sendFile(rootPath, splitedCmd, ctx, future -> {
            if (!future.isSuccess()) {
                future.cause().printStackTrace();
            }
            if (future.isSuccess()) {
                System.out.println("File uploaded successfully");
            }
        });

    }
}
