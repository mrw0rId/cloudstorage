package ru.geekbrains.cloudstorage.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.geekbrains.cloudstorage.NettyClient;
import ru.geekbrains.cloudstorage.util.AuthState;
import ru.geekbrains.cloudstorage.util.GUI;
import ru.geekbrains.cloudstorage.util.FileState;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

public class ServerAnswerHandler extends ChannelInboundHandlerAdapter {

    private final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private final GUI gui;
    private NettyClient nettyClient;

    private FileState currentFileState = FileState.IDLE;
    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;
    private Path rootPath;
    private String stringFileName = null;

    public ServerAnswerHandler(GUI gui, NettyClient nettyClient) {
        this.gui = gui;
        this.nettyClient = nettyClient;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;

        if (Stream.of("Deleted", "Not", "User", "Authorized", "Disconnected", "File", "-", "server", "Renamed", "Directory").
                anyMatch(s->buf.toString(StandardCharsets.UTF_8).startsWith(s))) {
            if(buf.toString(StandardCharsets.UTF_8)
                    .startsWith("Authorized")) {
                nettyClient.setAuthState(AuthState.AUTH);
                nettyClient.setRootPath(Paths.get("client" + File.separator + "root_folder"
                        + File.separator + buf.toString(StandardCharsets.UTF_8).split(" ")[1]));
                rootPath = Paths.get("client" + File.separator + "root_folder"
                        + File.separator + buf.toString(StandardCharsets.UTF_8).split(" ")[1]);
            }
            if(buf.toString(StandardCharsets.UTF_8)
                    .startsWith("Disconnected")) nettyClient
                    .setAuthState(AuthState.NOTAUTH);
            if (buf.toString(StandardCharsets.UTF_8)
                    .startsWith("-") || buf.toString(StandardCharsets.UTF_8)
                    .startsWith("server")) {
                gui.getChatArea().append(buf.resetReaderIndex().toString(StandardCharsets.UTF_8));
            }else gui.getChatArea()
                    .append(formatter.format(new Date()) +": " + buf.toString(StandardCharsets.UTF_8) + "\n");
            buf.clear();
        } else {
            while (buf.readableBytes() > 0) {
                if (currentFileState == FileState.IDLE) {
                    byte readed = buf.readByte();
                    if (readed == (byte) 25) {
                        currentFileState = FileState.NAME_LENGTH;
                        receivedFileLength = 0L;
                        System.out.println("STATE: Start file receiving");
                    } else if (buf.toString(StandardCharsets.UTF_8).startsWith("opc")) {
                        nettyClient.setCmd(buf.toString(StandardCharsets.UTF_8));
                        nettyClient.setSplitedCmd(buf.toString(StandardCharsets.UTF_8).split(" "));
                        nettyClient.commandController();
                        buf.clear();
                    } else {
                        System.out.println("ERROR: Invalid first byte - " + readed);
                    }
                }
                if (currentFileState == FileState.NAME_LENGTH) {
                    System.out.println("waiting nameLength");
                    if (buf.readableBytes() >= 4) {
                        System.out.println("STATE: Get filename length");
                        nextLength = buf.readInt();
                        currentFileState = FileState.NAME;
                    }
                }
                if (currentFileState == FileState.NAME) {
                    if (buf.readableBytes() >= nextLength) {
                        byte[] fileName = new byte[nextLength];
                        buf.readBytes(fileName);
                        stringFileName = new String(fileName, StandardCharsets.UTF_8);
                        System.out.println("STATE: Filename received - " + stringFileName);
                        out = new BufferedOutputStream(new FileOutputStream(rootPath + File.separator + stringFileName));
                        currentFileState = FileState.FILE_LENGTH;
                    }
                }
                if (currentFileState == FileState.FILE_LENGTH) {
                    if (buf.readableBytes() >= 8) {
                        fileLength = buf.readLong();
                        System.out.println("STATE: File length received - " + fileLength);
                        currentFileState = FileState.FILE;
                    }
                }
                if (currentFileState == FileState.FILE) {
                    while (buf.readableBytes() > 0) {
                        out.write(buf.readByte());
                        receivedFileLength++;
                        if (fileLength == receivedFileLength) {
                            currentFileState = FileState.IDLE;
                            System.out.println("File received");
                            gui.getChatArea().append(formatter.format(new Date()) + ": " + stringFileName + " downloaded successfully\n");
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
