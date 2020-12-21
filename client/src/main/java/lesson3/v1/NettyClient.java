package lesson3.v1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class NettyClient extends JFrame {

    private static JTextField text;

    private final Network network;
    private static NettyClient nettyClient;
    private String[] cmd;
    private String t;
    private Path rootPath = Paths.get("client" + File.separator + "root_folder");

    public static void main(String[] args) throws Exception {
        CountDownLatch networkStarter = new CountDownLatch(1);
        nettyClient = new NettyClient(networkStarter);
    }

    public NettyClient(CountDownLatch networkStarter) {
        this.network = new Network();
        new Thread(()->network.start(networkStarter)).start();
        createGUI();
    }

    public void run(Path path) throws IOException {
        if (cmd[0].equals("upload")) {
            sendFile(path, future -> {
                if (!future.isSuccess()) {
                    future.cause().printStackTrace();
//                    network.stop();
                }
                if (future.isSuccess()) {
                    text.setText("File uploaded successfully");
                    System.out.println("File uploaded successfully");
//                    network.stop();
                }
            });
        }
    }

    private void sendFile(Path path, ChannelFutureListener finishListener) throws IOException {
        Channel channel = network.getCurrentChannel();

        Stream<Path> pathStream = Files.find(path, 100, new BiPredicate<Path, BasicFileAttributes>() {
            @Override
            public boolean test(Path path, BasicFileAttributes basicFileAttributes) {
                return path.getFileName().toString().equals(cmd[1]);
            }
        });
        if (pathStream != null) {
            channel.writeAndFlush(Unpooled.copiedBuffer(t.getBytes()));
            for (var p : pathStream.toArray()) {
                Path fileString = (Path) p;
                FileRegion region = new DefaultFileRegion(fileString.toFile(), 0, Files.size(fileString));

                ByteBuf buf;

                byte[] filenameBytes = fileString.getFileName().toString().getBytes(StandardCharsets.UTF_8);
                buf = ByteBufAllocator.DEFAULT.directBuffer(4);
                buf.writeInt(filenameBytes.length);
                System.out.println(buf.readableBytes());
                channel.writeAndFlush(buf);
                System.out.println("sending filename length");

                buf = ByteBufAllocator.DEFAULT.directBuffer(filenameBytes.length);
                buf.writeBytes(filenameBytes);
                System.out.println(buf.readableBytes());
                channel.writeAndFlush(buf);
                System.out.println("sending filename");

                buf = ByteBufAllocator.DEFAULT.directBuffer(8);
                buf.writeLong(Files.size(fileString));
                System.out.println(buf.readableBytes());
                channel.writeAndFlush(buf);
                System.out.println("sending filesize");

                ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
                if (finishListener != null) {
                    transferOperationFuture.addListener(finishListener);
                }
            }
        } else {
            text.setText("file not found: " + path.getFileName());
            System.out.println("file not found: " + path.getFileName());
        }

    }

    private void createGUI() {
        setSize(300, 300);
        setTitle("FileTransfer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocation(500, 250);

        JButton send = new JButton("SEND");
        text = new JTextField();
        add(text, BorderLayout.CENTER);
        add(send, BorderLayout.SOUTH);
        text.setCaretColor(Color.green);

        send.addActionListener(a -> {
            try {
                t = text.getText();
                cmd = t.split(" ");
                if (cmd[0].equals("download") || cmd[0].equals("upload")) {
                    nettyClient.run(Paths.get(rootPath + File.separator + cmd[1]));
                } else {
                    network.getCurrentChannel().writeAndFlush(Unpooled.copiedBuffer(cmd[0].getBytes()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            text.setText("");
            text.grabFocus();
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                network.stop();
                super.windowClosed(e);
            }
        });
        setVisible(true);
    }
}
