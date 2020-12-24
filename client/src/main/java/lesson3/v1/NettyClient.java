package lesson3.v1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiPredicate;
import java.util.stream.Stream;

public class NettyClient extends JFrame {

    private static JTextField text;
    private JTextArea chatArea;

    private final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private Date date = new Date();

    private final Network network;
    private static NettyClient nettyClient;
    private String[] splitedCmd;
    private String cmd;
    private Path rootPath = Paths.get("client" + File.separator + "root_folder");
    private final String serviceCmd = "type:\n" +
            "<help> for list of service commands\n" +
            "<ls> for list of files\n" +
            "<download 'filename'> for getting file from server\n" +
            "<upload 'filename'> for uploading file to server\n" +
            "<exit> for quit program\n";

    public static void main(String[] args) throws Exception {
        CountDownLatch networkStarter = new CountDownLatch(1);
        nettyClient = new NettyClient(networkStarter);
    }

    public NettyClient(CountDownLatch networkStarter) {
        this.network = new Network();
        new Thread(() -> network.start(networkStarter)).start();
        createGUI();
    }

    public void run(Path path) throws IOException, InterruptedException {
        if (splitedCmd[0].equals("upload")) {
            ClientFileSender.sendFile(path, cmd, network.getCurrentChannel(), text, future -> {
                if (!future.isSuccess()) {
                    future.cause().printStackTrace();
//                    network.stop();
                }
                if (future.isSuccess()) {
                    chatArea.append(formatter.format(date) + splitedCmd[1] + " uploaded successfully\n");
                    System.out.println(formatter.format(date) + splitedCmd[1] + " uploaded successfully\n");
                }
            });
        } else if (splitedCmd[0].equals("download")) {
            Channel channel = network.getCurrentChannel();
            channel.writeAndFlush(Unpooled.copiedBuffer(cmd.getBytes()));
        }
    }

    private void commandController() {
        try {
            if (splitedCmd[0].equals("download") || splitedCmd[0].equals("upload")) {
                chatArea.append(formatter.format(date) + ": " + cmd + "\n");
                nettyClient.run(rootPath);
            } else if (splitedCmd[0].equals("help")) {
                chatArea.append(serviceCmd);
//                network.getCurrentChannel().writeAndFlush(Unpooled.copiedBuffer(splitedCmd[0].getBytes()));
            }
            //TODO: Добавить обработку служебных команд
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void createGUI() {
        setSize(300, 300);
        setTitle("FileTransfer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocation(500, 250);

        // Текстовое поле для вывода сообщений
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);

        // Нижняя панель с полем для ввода сообщений и кнопкой отправки сообщений
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JButton send = new JButton("SEND");
        JButton clear = new JButton("clear");
        bottomPanel.add(send, BorderLayout.WEST);
        bottomPanel.add(clear, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);
        text = new JTextField();
        bottomPanel.add(text, BorderLayout.CENTER);
        text.setCaretColor(Color.green);

        send.addActionListener(a -> {
            cmd = text.getText();
            splitedCmd = cmd.split(" ");

            commandController();

            text.setText("");
            text.grabFocus();
        });

        clear.addActionListener(a -> {
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
