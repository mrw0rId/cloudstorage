package ru.geekbrains.cloudstorage.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.geekbrains.cloudstorage.NettyClient;
import ru.geekbrains.cloudstorage.util.State;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClientFileGetterHandler extends ChannelInboundHandlerAdapter {

    private final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private Date date = new Date();
    private NettyClient.GUI gui;

    private State currentState = State.IDLE;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;
    private Path rootPath = Paths.get("client" + File.separator + "root_folder");
    private String stringFileName = null;

    public ClientFileGetterHandler(NettyClient.GUI gui) {
        this.gui = gui;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        if (buf.toString(StandardCharsets.UTF_8).startsWith("Deleted") || buf.toString(StandardCharsets.UTF_8).startsWith("Not")) {
            gui.getChatArea().append(formatter.format(date) +": " + buf.toString(StandardCharsets.UTF_8));
            buf.clear();
        } else {
            while (buf.readableBytes() > 0) {
                if (currentState == State.IDLE) {
                    byte readed = buf.readByte();
                    if (readed == (byte) 25) {
                        currentState = State.NAME_LENGTH;
                        receivedFileLength = 0L;
                        System.out.println("STATE: Start file receiving");
                    } else if (buf.resetReaderIndex().toString(StandardCharsets.UTF_8).startsWith("-") || buf.resetReaderIndex().toString(StandardCharsets.UTF_8).startsWith("server")) {
                        gui.getChatArea().append(buf.resetReaderIndex().toString(StandardCharsets.UTF_8));
                        buf.clear();
                    } else {
                        System.out.println("ERROR: Invalid first byte - " + readed);
                    }
                }
                if (currentState == State.NAME_LENGTH) {
                    System.out.println("waiting nameLength");
                    if (buf.readableBytes() >= 4) {
                        System.out.println("STATE: Get filename length");
                        nextLength = buf.readInt();
                        currentState = State.NAME;
                    }
                }
                if (currentState == State.NAME) {
                    if (buf.readableBytes() >= nextLength) {
                        byte[] fileName = new byte[nextLength];
                        buf.readBytes(fileName);
                        stringFileName = new String(fileName, StandardCharsets.UTF_8);
                        System.out.println("STATE: Filename received - " + stringFileName);
                        out = new BufferedOutputStream(new FileOutputStream(rootPath + File.separator + stringFileName));
                        currentState = State.FILE_LENGTH;
                    }
                }
                if (currentState == State.FILE_LENGTH) {
                    if (buf.readableBytes() >= 8) {
                        fileLength = buf.readLong();
                        System.out.println("STATE: File length received - " + fileLength);
                        currentState = State.FILE;
                    }
                }
                if (currentState == State.FILE) {
                    while (buf.readableBytes() > 0) {
                        out.write(buf.readByte());
                        receivedFileLength++;
                        if (fileLength == receivedFileLength) {
                            currentState = State.IDLE;
                            System.out.println("File received");
                            gui.getChatArea().append(formatter.format(date) + ": " + stringFileName + " downloaded successfully\n");
                            out.close();
                            break;
                        }
                    }
                }
            }
            if (buf.readableBytes() == 0) {
                buf.release();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
