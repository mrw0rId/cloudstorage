package ru.geekbrains.cloudstorage.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.geekbrains.cloudstorage.auth.AuthService;
import ru.geekbrains.cloudstorage.util.AuthState;
import ru.geekbrains.cloudstorage.util.FileState;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ClientCommandsHandler extends ChannelInboundHandlerAdapter {

    private final AuthService authService;
    private AuthState authState = AuthState.NOTAUTH;
    private AuthService.Record currentUser;

    private FileState fileReceivingState = FileState.START;
    private String cmd;
    private String[] splitedCmd;

    private int nextLength;
    private long fileLength;
    private long receivedFileLength;
    private BufferedOutputStream out;
    private Path rootPath;

    public ClientCommandsHandler(AuthService authService) {
        this.authService = authService;
    }

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
        if (fileReceivingState == FileState.START) {
            StringBuilder sb = new StringBuilder();
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            cmd = sb.append(new String(bytes)).toString();
            System.out.println(cmd);
            splitedCmd = cmd.split(" ");
        }
        //TODO: добовить доступ к операциям в заисимости от роли пользователя
        if (authState == AuthState.NOTAUTH) {
            if(splitedCmd[0].equals("auth")){
                System.out.println(1);
                AuthService.Record possibleRecord = authService.findRecord(splitedCmd[1], splitedCmd[2]);
                if(possibleRecord!=null){
                    System.out.println(9);
                    if(possibleRecord.getId()==0){
                        System.out.println(10);
                        ctx.writeAndFlush(Unpooled
                                .copiedBuffer("User is already authorized"
                                        .getBytes()));
                    }else {
                        System.out.println(11);
                        currentUser = possibleRecord;
                        ctx.writeAndFlush(Unpooled
                                .copiedBuffer(("Authorized: "+ splitedCmd[1])
                                        .getBytes()));
                        rootPath = Paths.get("server" + File.separator + "root_folder" + File.separator + currentUser.getLogin());
                        if(!rootPath.toFile().exists()) rootPath.toFile().mkdirs();
                        authState = AuthState.AUTH;
                    }
                }else ctx.writeAndFlush(Unpooled.copiedBuffer("User not found".getBytes()));
            }
        } else {
            switch (splitedCmd[0]) {
                case "upload":
                    if (fileReceivingState == FileState.START) fileReceivingState = FileState.IDLE;
                    System.out.println("Starting upload");

                    String stringFileName;
                    while (buf.readableBytes() > 0) {
                        if (fileReceivingState == FileState.IDLE) {
                            byte readed = buf.readByte();
                            if (readed == (byte) 25) {
                                fileReceivingState = FileState.NAME_LENGTH;
                                receivedFileLength = 0L;
                                System.out.println("STATE: Start file receiving");
                            } else {
                                System.out.println("ERROR: Invalid first byte - " + readed);
                            }
                        }
                        if (fileReceivingState == FileState.NAME_LENGTH) {
                            System.out.println("waiting nameLength");
                            if (buf.readableBytes() >= 4) {
                                System.out.println("STATE: Get filename length");
                                nextLength = buf.readInt();
                                fileReceivingState = FileState.NAME;
                            }
                        }
                        if (fileReceivingState == FileState.NAME) {
                            if (buf.readableBytes() >= nextLength) {
                                byte[] fileName = new byte[nextLength];
                                buf.readBytes(fileName);
                                stringFileName = new String(fileName, StandardCharsets.UTF_8);
                                System.out.println("STATE: Filename received - " + stringFileName);
                                out = new BufferedOutputStream(new FileOutputStream(rootPath + File.separator + stringFileName));
                                fileReceivingState = FileState.FILE_LENGTH;
                            }
                        }
                        if (fileReceivingState == FileState.FILE_LENGTH) {
                            if (buf.readableBytes() >= 8) {
                                fileLength = buf.readLong();
                                System.out.println("STATE: File length received - " + fileLength);
                                fileReceivingState = FileState.FILE;
                            }
                        }
                        if (fileReceivingState == FileState.FILE) {
                            while (buf.readableBytes() > 0) {
                                out.write(buf.readByte());
                                receivedFileLength++;
                                if (fileLength == receivedFileLength) {
                                    fileReceivingState = FileState.START;
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
                case "ops":
                case "download":
                    System.out.println("starting download");
                    ServerFileManager.cmdController(rootPath, cmd, ctx, future -> {
                        if (!future.isSuccess()) {
                            future.cause().printStackTrace();
                        }
                        if (future.isSuccess()) {
                            System.out.println("File uploaded successfully");
                        }
                    });
                    break;
                case "lss":
                    ctx.writeAndFlush(Unpooled.copiedBuffer("------LIST OF SERVER FILES-----\n".getBytes()));
                    System.out.println(rootPath);
                    Files.walk(rootPath)
                            .forEach(s -> ctx.writeAndFlush(Unpooled.copiedBuffer((s.toString() + "\n")
                                    .getBytes())));
                    ctx.writeAndFlush(Unpooled.copiedBuffer("--------------------------------------------\n".getBytes()));
                    break;
                case "rms":
                case "rns":
                    ServerFileManager.cmdController(rootPath, cmd, ctx, future -> {});
                    break;
                case "lo":
                    authService.logout(currentUser);
                    authState = AuthState.NOTAUTH;
                    ctx.writeAndFlush(Unpooled
                            .copiedBuffer(("Disconnected: " + currentUser.getLogin())
                                    .getBytes()));
                    break;
                case "exit":
                    authService.logout(currentUser);
                    break;
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if(cause.getClass().equals(SocketException.class)){
            authService.logout(currentUser);
            System.out.println("Connection interrupted");
        }
        ctx.close();
    }
}
