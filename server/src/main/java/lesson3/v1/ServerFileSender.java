package lesson3.v1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class ServerFileSender {
    public static void sendFile(Path path, String[] splitedCmd, ChannelHandlerContext ctx, ChannelFutureListener finishListener) throws IOException, InterruptedException {
        Stream<Path> pathStream = Files.find(path, 100, new BiPredicate<Path, BasicFileAttributes>() {
            @Override
            public boolean test(Path path, BasicFileAttributes basicFileAttributes) {
                return path.getFileName().toString().equals(splitedCmd[1]);
            }
        });
        if (pathStream != null) {
            for (var p : pathStream.toArray()) {
                Path fileString = (Path) p;
                FileRegion region = new DefaultFileRegion(fileString.toFile(), 0, Files.size(fileString));

                ByteBuf buf;

                buf = ByteBufAllocator.DEFAULT.directBuffer(1);
                buf.writeByte((byte) 25);
                ctx.writeAndFlush(buf);

                Thread.sleep(2000);
                byte[] filenameBytes = fileString.getFileName().toString().getBytes(StandardCharsets.UTF_8);
                buf = ByteBufAllocator.DEFAULT.directBuffer(4);
                buf.writeInt(filenameBytes.length);
                ctx.writeAndFlush(buf);
                System.out.println("sending filename length");
                Thread.sleep(2000);

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
