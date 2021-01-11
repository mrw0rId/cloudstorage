package ru.geekbrains.cloudstorage.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class ServerFileManager {

    public static void deleteFile(Path rootPath, String filePath, ChannelHandlerContext ctx) {
        String[] splitedCmd = filePath.split(Pattern.quote(File.separator));
        Path directory;
        String filename;

        try {
            directory = Paths.get(splitedCmd[0]);
            filename = splitedCmd[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            directory = rootPath;
            filename = splitedCmd[0];
        }

        try {
            Path finalDirectory = directory;
            String finalFilename = filename;
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
                    System.out.println(file.getFileName().toString());
                    if (file.getFileName().toString().equals(finalFilename)) {
                        try {
                            Files.delete(file);
                            ctx.writeAndFlush(Unpooled.copiedBuffer(("Deleted file: " + file.getFileName() + "\n").getBytes()));
                            return FileVisitResult.TERMINATE;
                        } catch (NoSuchFileException x) {
                            ctx.writeAndFlush(Unpooled.copiedBuffer(("Not found: " + file.getFileName() + "\n").getBytes()));
                        }
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.err.println(e);
        }
    }

    public static void sendFile(Path path, String[] splitedCmd, ChannelHandlerContext ctx, ChannelFutureListener finishListener) throws IOException, InterruptedException {
        Stream<Path> pathStream = Files.find(path, 100, (path1, basicFileAttributes) ->
                path1.getFileName().toString().equals(splitedCmd[1]));
        if (pathStream != null) {
            for (var p : pathStream.toArray()) {
                Path fileString = (Path) p;
                FileRegion region = new DefaultFileRegion(fileString.toFile(), 0, Files.size(fileString));

                ByteBuf buf;

                buf = ByteBufAllocator.DEFAULT.directBuffer(1);
                buf.writeByte((byte) 25);
                ctx.writeAndFlush(buf);

                byte[] filenameBytes = fileString.getFileName().toString().getBytes(StandardCharsets.UTF_8);
                buf = ByteBufAllocator.DEFAULT.directBuffer(4);
                buf.writeInt(filenameBytes.length);
                ctx.writeAndFlush(buf);
                System.out.println("sending filename length");

                buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
                buf.writeBytes(filenameBytes);
                ctx.writeAndFlush(buf);
                System.out.println("sending filename");

                buf = ByteBufAllocator.DEFAULT.directBuffer(8);
                buf.writeLong(Files.size(fileString));
                System.out.println(buf.readableBytes());
                ctx.writeAndFlush(buf);
                System.out.println("sending filesize");

                ChannelFuture transferOperationFuture = ctx.writeAndFlush(region);
                if (finishListener != null) {
                    transferOperationFuture.addListener(finishListener);
                }
            }
        } else {
            System.out.println("file not found: " + path.getFileName());
        }
    }
}
