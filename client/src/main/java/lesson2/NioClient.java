package lesson2;

import lesson1.Client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.SQLOutput;
import java.util.Arrays;
import java.util.Iterator;

public class NioClient extends JFrame {

    private final SocketChannel socketChannel;
    private final Selector selector;
    private final ByteBuffer buf = ByteBuffer.allocate(8000);
    private String[] cmd;
    private int flag = 0;

    public NioClient() throws IOException {
        this.socketChannel = SocketChannel.open();
        this.socketChannel.configureBlocking(false);
        this.selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        this.socketChannel.connect(new InetSocketAddress("localhost", 8089));


        while (true) {
            Iterator<SelectionKey> iter;
            SelectionKey key;
            while (this.socketChannel.isOpen()) {
                selector.select();
                iter = this.selector.selectedKeys().iterator();
                while (iter.hasNext()) {
                    key = iter.next();
                    iter.remove();
                    if (key.isConnectable()) {
                        socketChannel.finishConnect();
                        new Thread(this::createGUI).start();
                        socketChannel.register(selector, SelectionKey.OP_READ);
                    }
                    if(key.isReadable() && cmd == null){
                        SocketChannel ch = (SocketChannel) key.channel();
                        int read = 0;
                        while ((read = ch.read(buf)) > 0) {
                            buf.flip();
                            byte[] bytes = new byte[buf.limit()];
                            buf.get(bytes);
                            System.out.println((new String(bytes)));
                        }
                        buf.clear();
                    }
                    if (key.isReadable() && cmd!=null) {
                        Path file = Paths.get("client" + File.separator + "root_folder" + File.separator + cmd[1]);
                        if (cmd[0].equals("upload")) {
                            file = Paths.get("server" + File.separator + "root_folder" + File.separator + cmd[1]);
                        }
                        SocketChannel ch = (SocketChannel) key.channel();
                        RandomAccessFile src = new RandomAccessFile(file.toFile(), "rw");
                        FileChannel srcChannel = src.getChannel();
                        int read = 0;
                        while ((read = ch.read(buf)) > 0) {
                            buf.flip();
                            while (buf.hasRemaining()) {
                                srcChannel.write(buf);
                            }
                            buf.clear();
                        }
                        srcChannel.close();
                        cmd = null;
                        try {
                            wait(1000);
                        } catch (InterruptedException | IllegalMonitorStateException e) {
                            continue;
                        }
                    }

                }
            }

//                else if (sk.isReadable()){
//                    SocketChannel ch = (SocketChannel) sk.channel();
//                    ch.write(ByteBuffer.wrap(cmd.getBytes()));
//                    buf.clear();
//                    socketChannel.read(buf);
//                    buf.flip();
//                    Path file = Paths.get("client"+File.separator + "root_folder" + cmdSplited[1]);
//                    RandomAccessFile src = new RandomAccessFile(file.toFile(), "rw");
//                    FileChannel srcChannel = src.getChannel();
//                    while (buf.hasRemaining()){
//                        srcChannel.write(buf);
//                    }
//                }else if (sk.isWritable()){
//                    ByteBuffer command = ByteBuffer.wrap(cmd.getBytes());
//                    while (command.hasRemaining()){
//                        socketChannel.write(command);
//                    }
//                    sk.interestOps(SelectionKey.OP_READ);
//                }

        }
    }

    private void createGUI() {
        setSize(300, 300);
        setTitle("FileTransfer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocation(500, 250);

        JButton send = new JButton("SEND");
        JTextField text = new JTextField();
        add(text, BorderLayout.CENTER);
        add(send, BorderLayout.SOUTH);
        text.setCaretColor(Color.green);

        send.addActionListener(a -> {
            try {
                socketChannel.register(selector, SelectionKey.OP_WRITE);
                String t = text.getText();
                cmd = t.split(" ");
                socketChannel.write(ByteBuffer.wrap(t.getBytes()));
                socketChannel.register(selector, SelectionKey.OP_READ);
            } catch (IOException e) {
                e.printStackTrace();
            }
            text.setText("");
            text.grabFocus();
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    socketChannel.close();
                    super.windowClosed(e);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
        });
        setVisible(true);
    }

    public static void main(String[] args) throws IOException {
        new NioClient();
    }
}
