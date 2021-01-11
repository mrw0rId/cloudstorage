package ru.geekbrains.cloudstorage.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import ru.geekbrains.cloudstorage.util.ManagerFlag;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;

public class ClientFileManager {

    private static ManagerFlag state = ManagerFlag.IDLE;

    public static void cmdController(Path rootPath, String flag, String filePath, JTextArea chatArea, String date, Channel channel, ChannelFutureListener finishListener) {

        switch (flag) {
            case "rmc":
                state = ManagerFlag.DELETE;
                manageFile(rootPath, filePath, chatArea, date, channel, finishListener);
                break;
            case "opc":
                state = ManagerFlag.OPEN;
                manageFile(rootPath, filePath, chatArea, date, channel, finishListener);
                break;
            case "upload":
                state = ManagerFlag.SEND;
                manageFile(rootPath, filePath, chatArea, date, channel, finishListener);
                break;
        }
    }

    public static void manageFile(Path rootPath, String filePath, JTextArea chatArea, String date, Channel channel, ChannelFutureListener finishListener) {
        String[] splitedCmd = filePath.split(Pattern.quote(File.separator));
        Path directory;
        String fileName;

        try {
            directory = Paths.get(splitedCmd[0]);
            fileName = splitedCmd[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            directory = rootPath;
            fileName = splitedCmd[0];
        }

        try {
            Path finalDirectory = directory;
            String finalFileName = fileName;
            System.out.println("finalFileName:" + finalFileName);
            Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    System.out.println("pre visit dir:" + dir);
                    if (!finalDirectory.equals(rootPath)) {
                        if (!dir.endsWith(finalDirectory) && !dir.equals(rootPath)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        } else return FileVisitResult.CONTINUE;
                    } else return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (file.getFileName().toString().equals(finalFileName)) {
                        try {
                            switch (state) {
                                case DELETE:
                                    Files.delete(file);
                                    chatArea.append(date + ": Deleted file: " + file.getFileName() + "\n");
                                    return FileVisitResult.TERMINATE;
                                case OPEN:
                                    File f = new File(String.valueOf(file.toAbsolutePath()));
                                    Desktop desktop = Desktop.getDesktop();
                                    desktop.open(f);
                                    chatArea.append(date + ": Opening file: " + file.getFileName() + "\n");
                                    return FileVisitResult.TERMINATE;
                                case SEND:
                                    channel.writeAndFlush(Unpooled.copiedBuffer(("upload ").getBytes()));

                                    FileRegion region = new DefaultFileRegion(file.toFile(), 0, Files.size(file));

                                    ByteBuf buf;
                                    //Без sleep команда серверу - upload(выше) и сигнальный байт(ниже) сливаются
                                    // и читаются на сервере единой пачкой, поэтому метод загрузки файла(upload)
                                    // на сервере не получает свойсигнальный байт.
                                    Thread.sleep(100);
                                    buf = ByteBufAllocator.DEFAULT.directBuffer(1);
                                    buf.writeByte((byte) 25);
                                    channel.writeAndFlush(buf);

                                    byte[] filenameBytes = file.getFileName().toString().getBytes(StandardCharsets.UTF_8);
                                    buf = ByteBufAllocator.DEFAULT.directBuffer(4);
                                    buf.writeInt(filenameBytes.length);
                                    channel.writeAndFlush(buf);
                                    System.out.println("sending filename length");

                                    buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
                                    buf.writeBytes(filenameBytes);
                                    System.out.println(buf.readableBytes());
                                    channel.writeAndFlush(buf);
                                    System.out.println("sending filename");

                                    buf = ByteBufAllocator.DEFAULT.directBuffer(8);
                                    buf.writeLong(Files.size(file));
                                    System.out.println(buf.readableBytes());
                                    channel.writeAndFlush(buf);
                                    System.out.println("sending filesize");

                                    ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
                                    if (finishListener != null) {
                                        transferOperationFuture.addListener(finishListener);
                                    }
                                    return FileVisitResult.TERMINATE;
                            }
                        } catch (NoSuchFileException | InterruptedException x) {
                            chatArea.append(date + ": File not found: " + finalFileName + "\n");
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println(e);
        }
    }
}
