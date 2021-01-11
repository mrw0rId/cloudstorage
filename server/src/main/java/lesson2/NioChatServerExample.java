package lesson2;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class NioChatServerExample implements Runnable {

    private final ServerSocketChannel serverSocketChannel;
    private final Selector selector;
    private int acceptedClientIndex = 1;
    private final ByteBuffer welcomeBuf = ByteBuffer.wrap("Добро пожаловать в чат!\n".getBytes());
    private ByteBuffer cmdBuffer = ByteBuffer.allocate(1024);
    private ByteBuffer fileBuffer = ByteBuffer.allocate(8000);
    private String clientName;
    private String[] cmd;

    NioChatServerExample() throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.serverSocketChannel.socket().bind(new InetSocketAddress(8089));
        this.serverSocketChannel.configureBlocking(false);
        this.selector = Selector.open();
        this.serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
    }

    public static void main(String[] args) throws IOException {
        new Thread(new NioChatServerExample()).start();
    }

    @Override
    public void run() {
        try {
            System.out.println("Сервер запущен (Порт: 8089)");
            Iterator<SelectionKey> iter;
            SelectionKey key;
            while (this.serverSocketChannel.isOpen()) {
                selector.select();
                iter = this.selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    key = iter.next();
                    if (key.isAcceptable()) {
                        handleAccept(key);
                    }
                    if (key.isReadable()) {
                            handleRead(key);
                    }
                    if (key.isWritable()) {
                        System.out.println("3");
                        getFile(cmd[1], key);
                    }
                    iter.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        SocketChannel sc = ((ServerSocketChannel) key.channel()).accept();
        clientName = "Клиент #" + acceptedClientIndex;
        acceptedClientIndex++;
        sc.configureBlocking(false);
        sc.register(selector, SelectionKey.OP_READ, clientName);
        sc.write(welcomeBuf);
        welcomeBuf.rewind();
        System.out.println("Подключился новый клиент " + clientName);
    }

    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel ch = (SocketChannel) key.channel();
        StringBuilder sb = new StringBuilder();

        cmdBuffer.clear();
        int read = 0;
        while ((read = ch.read(cmdBuffer)) > 0) {
            cmdBuffer.flip();
            byte[] bytes = new byte[cmdBuffer.limit()];
            cmdBuffer.get(bytes);
            sb.append(new String(bytes));
            cmdBuffer.clear();
        }
        String msg;
        if (read < 0) {
            msg = key.attachment() + " покинул чат\n";
            ch.close();
        } else {
            msg = key.attachment() + ": " + sb.toString();
        }
        System.out.println(msg);
//        broadcastMessage(msg, key);
        String a = sb.toString().trim();
        cmd = a.split(" ");
        if (cmd[0].equals("download") || cmd[0].equals("upload")) {
            ch.register(selector,SelectionKey.OP_WRITE);
        }
    }

    private void placeFile(SelectionKey key) throws IOException {
        Path file = Paths.get("client" + File.separator + "root_folder"+ File.separator + cmd[1]);
        if (cmd[0].equals("upload")) {
            file = Paths.get("server" + File.separator + "root_folder"+ File.separator + cmd[1]);
        }
        SocketChannel ch = (SocketChannel) key.channel();
        ch.read(fileBuffer);
        fileBuffer.flip();
        RandomAccessFile src = new RandomAccessFile(file.toFile(), "rw");
        FileChannel srcChannel = src.getChannel();
        while (fileBuffer.hasRemaining()) {
            srcChannel.write(fileBuffer);
        }
        fileBuffer.clear();
        cmd = null;
    }

        private void getFile (String fileName, SelectionKey key) throws IOException {
            Path rootPath = Paths.get("server" + File.separator + "root_folder");
            if (cmd[0].equals("upload")) {
                rootPath = Paths.get("client" + File.separator + "root_folder");
            }
            Stream<Path> pathStream = Files.find(rootPath, 100, new BiPredicate<Path, BasicFileAttributes>() {
                @Override
                public boolean test(Path path, BasicFileAttributes basicFileAttributes) {
                    return path.getFileName().toString().equals(fileName);
                }
            });
            for (Object p:pathStream.toArray()) {
                Path fileString = Paths.get(p.toString());
                if (fileString.getFileName().toString().equals(fileName)) {
                    System.out.println("file found at path: " + fileString.toAbsolutePath());

                    SocketChannel sch = (SocketChannel) key.channel();
                    RandomAccessFile src = new RandomAccessFile(fileString.toFile(), "rw");
                    FileChannel srcChannel = src.getChannel();
                    int bytesRead = 0;
                    while (srcChannel.read(fileBuffer)>0){
                        fileBuffer.flip();
                        while (fileBuffer.hasRemaining()) {
                            sch.write(fileBuffer);
                        }
                        fileBuffer.clear();
                    }
                    cmd = null;
                    sch.register(selector, SelectionKey.OP_READ);
                }
            }

//            try {
//                Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
//                    @Override
//                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
//                        String fileString = file.toAbsolutePath().toString();
//                        System.out.println(file.getFileName().toString());
//
//
//                        if (file.getFileName().toString().equals(fileName)) {
//                            System.out.println("file found at path: " + file.toAbsolutePath());
//
//                            SocketChannel sch = (SocketChannel) key.channel();
//                            RandomAccessFile src = new RandomAccessFile(file.toFile(), "rw");
//                            FileChannel srcChannel = src.getChannel();
//                            int bytesRead = srcChannel.read(fileBuffer);
//                            fileBuffer.flip();
//                            while (fileBuffer.hasRemaining()) {
//                                sch.write(fileBuffer);
//                            }
//                            fileBuffer.clear();
//                            key.interestOps(SelectionKey.OP_READ);
//                            return FileVisitResult.TERMINATE;
//                        }
//                        return FileVisitResult.CONTINUE;
//                    }
//                });
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        }

        private void broadcastMessage (String msg, SelectionKey k) throws IOException {
            ByteBuffer msgBuf = ByteBuffer.wrap(msg.getBytes());
            for (SelectionKey key : selector.keys()) {
                if (key.isValid() && key.channel() instanceof SocketChannel && !key.equals(k)) {
                    SocketChannel sch = (SocketChannel) key.channel();
                    sch.write(msgBuf);
                    msgBuf.rewind();
                }
            }
        }

    }