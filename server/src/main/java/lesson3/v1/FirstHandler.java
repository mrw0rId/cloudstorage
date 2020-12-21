package lesson3.v1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
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
        if(firstHandlerState==State.IDLE) {
            StringBuilder sb = new StringBuilder();
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            cmd = sb.append(new String(bytes)).toString();
            System.out.println(cmd);
            splitedCmd = cmd.split(" ");
        }
        if(cmd.startsWith("help")){
            System.out.println("type:\n" +
                    "<help> for list of service commands\n" +
                    "<ls> for list of files" +
                    "<download 'filename'> for getting file from server\n" +
                    "<upload 'filename'> for uploading file to server\n" +
                    "<exit> for quit program\n");
//            ctx.writeAndFlush("type:" +
//                    "<help> for list of service commands" +
//                    "<ls> for list of files" +
//                    "<download 'filename'> for getting file from server" +
//                    "<upload 'filename'> for uploading file to server" +
//                    "<exit> for quit program");
        } else if(cmd.startsWith("upload")){
            System.out.println("starting upload");
            if(firstHandlerState==State.IDLE){
                ByteBuf b;
                buf = ByteBufAllocator.DEFAULT.directBuffer(1);
                buf.writeByte((byte) 25);
                ctx.fireChannelRead(buf);
                firstHandlerState = State.FILE;
            }else{
                System.out.println("count:"+count);
                if(count<4){
                    ctx.fireChannelRead(buf);
                    count++;
                }else {
                    firstHandlerState=State.IDLE;
                    count=0;
                }
            }

        }else if(cmd.startsWith("download")){

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    public static void setFirstHandlerState(State firstHandlerState) {
        FirstHandler.firstHandlerState = firstHandlerState;
    }
}
