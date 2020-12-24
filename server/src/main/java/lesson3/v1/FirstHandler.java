package lesson3.v1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class FirstHandler extends ChannelInboundHandlerAdapter {

    private static State firstHandlerState = State.IDLE;
    private String cmd;
    private String[] splitedCmd;
    private static int count = 0;

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected...");
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client disconnected...");
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("first handler");
        ByteBuf buf = (ByteBuf) msg;
        if (firstHandlerState == State.IDLE) {
            StringBuilder sb = new StringBuilder();
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            cmd = sb.append(new String(bytes)).toString();
            System.out.println(cmd);
            splitedCmd = cmd.split(" ");
        }
        //TODO: Добавить обработку служебных команд
        if (splitedCmd[0].equals("upload")) {
            System.out.println("starting upload");
            if (count < 1) {
                count++;
                firstHandlerState = State.FILE;
            } else {
                if (count < 6) {
                    ctx.fireChannelRead(buf);
                    count++;
                }
                if (count == 6) {
                    firstHandlerState = State.IDLE;
                    count = 0;
                }
            }
        } else if (splitedCmd[0].equals("download")) {
            System.out.println("starting download");
            buf.resetReaderIndex();
            ctx.writeAndFlush(buf);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
