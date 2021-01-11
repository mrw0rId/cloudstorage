package ru.geekbrains.cloudstorage.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.geekbrains.cloudstorage.util.State;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientCommandsHandler extends ChannelInboundHandlerAdapter {

    private State firstHandlerState = State.START;
    private String cmd;
    private String[] splitedCmd;
    private int count = 0;

    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;
    private Path rootPath = Paths.get("server" + File.separator + "root_folder");

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
        ByteBuf buf = (ByteBuf) msg;
        if (firstHandlerState == State.START) {
            StringBuilder sb = new StringBuilder();
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            cmd = sb.append(new String(bytes)).toString();
            System.out.println(cmd);
            splitedCmd = cmd.split(" ");
        }
        //TODO: Добавить обработку служебных команд
        switch (splitedCmd[0]){
            case "upload":
                if(firstHandlerState==State.START) firstHandlerState = State.IDLE;
                System.out.println("Starting upload");

                String stringFileName;
                while (buf.readableBytes() > 0) {
                    if (firstHandlerState == State.IDLE) {
                        byte readed = buf.readByte();
                        if (readed == (byte) 25) {
                            firstHandlerState = State.NAME_LENGTH;
                            receivedFileLength = 0L;
                            System.out.println("STATE: Start file receiving");
                        } else {
                            System.out.println("ERROR: Invalid first byte - " + readed);
                        }
                    }
                    if (firstHandlerState == State.NAME_LENGTH) {
                        System.out.println("waiting nameLength");
                        if (buf.readableBytes() >= 4) {
                            System.out.println("STATE: Get filename length");
                            nextLength = buf.readInt();
                            firstHandlerState = State.NAME;
                        }
                    }
                    if (firstHandlerState == State.NAME) {
                        if (buf.readableBytes() >= nextLength) {
                            byte[] fileName = new byte[nextLength];
                            buf.readBytes(fileName);
                            stringFileName = new String(fileName, StandardCharsets.UTF_8);
                            System.out.println("STATE: Filename received - " + stringFileName);
                            out = new BufferedOutputStream(new FileOutputStream(rootPath + File.separator + stringFileName));
                            firstHandlerState = State.FILE_LENGTH;
                        }
                    }
                    if (firstHandlerState == State.FILE_LENGTH) {
                        if (buf.readableBytes() >= 8) {
                            fileLength = buf.readLong();
                            System.out.println("STATE: File length received - " + fileLength);
                            firstHandlerState = State.FILE;
                        }
                    }
                    if (firstHandlerState == State.FILE) {
                        while (buf.readableBytes() > 0) {
                            out.write(buf.readByte());
                            receivedFileLength++;
                            if (fileLength == receivedFileLength) {
                                firstHandlerState = State.START;
                                System.out.println("File received");
                                out.close();
                                break;
                            }
                        }
                    }
                }
                if (buf.readableBytes() == 0) {
                    buf.release();
                }
                break;
            case "download":
                System.out.println("starting download");
                ServerFileManager.sendFile(rootPath, splitedCmd, ctx, future -> {
                    if (!future.isSuccess()) {
                        future.cause().printStackTrace();
                    }
                    if (future.isSuccess()) {
                        System.out.println("File uploaded successfully");
                    }
                });
                break;
            case  "lss":
                ctx.writeAndFlush(Unpooled.copiedBuffer("------LIST OF SERVER FILES-----\n".getBytes()));
                Files.walk(rootPath).filter(Files::isRegularFile).forEach(s -> ctx.writeAndFlush(Unpooled.copiedBuffer((s.toString() + "\n").getBytes())));
                ctx.writeAndFlush(Unpooled.copiedBuffer("--------------------------------------------\n".getBytes()));
                break;
            case "rms":
                ServerFileManager.deleteFile(rootPath, splitedCmd[1], ctx);
                break;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
