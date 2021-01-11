package ru.geekbrains.cloudstorage;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import ru.geekbrains.cloudstorage.handlers.ClientFileManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class NettyClient {

    private final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    private Date date = new Date();

    private final Network network;
    private static NettyClient nettyClient;
    private GUI gui;
    private String[] splitedCmd;
    private String cmd;
    private final Path rootPath = Paths.get("client" + File.separator + "root_folder");
    private final String serviceCmd = "type:\n" +
            "<help> for list of service commands\n" +
            "<ex> for commands example\n" +
            "<lss> for list of files on server\n" +
            "<lsc> for list of files on client\n" +
            "<download 'filename'> for getting file from server\n" +
            "<upload 'filename'> for uploading file to server\n" +
            "<rms 'target folder/filename or filename'> for deleting file from server\n" +
            "<rmc 'target folder/filename or filename'> for deleting file from client\n" +
            "<rnc 'target folder/filename or filename newFileName'> for renaming file on client\n" +
            "<ops 'target folder/filename or filename'> for opening file to server\n" +
            "<opc 'target folder/filename or filename'> for opening file to server\n" +
            "<exit> for quit program\n";
    private final String example ="\n------Commands example-------\n" +
            "help\n" +
            "download 1.txt\n" +
            "upload 1.txt\n" +
            "rmc 1.txt\n" +
            "rmc folder\\1.txt\n" +
            "rmc 1.txt\n" +
            "rnc folder\\1.txt 3.txt\n" +
            "exit\n"+
            "----------------------------------------\n";

    public static void main(String[] args) throws Exception {
        CountDownLatch networkStarter = new CountDownLatch(1);
        nettyClient = new NettyClient(networkStarter);
    }

    public NettyClient(CountDownLatch networkStarter) {
        this.network = new Network();
        gui = new GUI();
        new Thread(() -> network.start(networkStarter, gui)).start();
    }

    private void commandController() {
        try {
            switch (splitedCmd[0]){
                case "help":
                    gui.getChatArea().append(serviceCmd);
                    break;
                case "ex":
                    gui.getChatArea().append(example);
                    break;
                case "download":
                    Channel channel = network.getCurrentChannel();
                    channel.writeAndFlush(Unpooled.copiedBuffer(cmd.getBytes()));
                    break;
                case "upload":
                    appendCmd(cmd,"");
                    ClientFileManager.cmdController(rootPath, cmd, gui.getChatArea(),
                            formatter.format(date), network.getCurrentChannel(), future -> {
                        if (!future.isSuccess()) {
                            future.cause().printStackTrace();
                        }
                        if (future.isSuccess()) {
                            appendCmd(splitedCmd[1]," uploaded successfully");
                            System.out.println(formatter.format(date) + ": " + splitedCmd[1] + " uploaded successfully\n");
                        }
                    });
                    break;
                case "lsc":
                    gui.getChatArea().append("------LIST OF CLIENT FILES-------\n");
                    Files.walk(rootPath).forEach(s -> gui.getChatArea().append(s.toString()+"\n"));
                    gui.getChatArea().append("--------------------------------------------\n");
                    break;
                case "lss":
                    network.getCurrentChannel().writeAndFlush(Unpooled.copiedBuffer(splitedCmd[0].getBytes()));
                    break;
                case "rms":
                    appendCmd(cmd, "");
                    network.getCurrentChannel().writeAndFlush(Unpooled.copiedBuffer(cmd.getBytes()));
                    break;
                case "rmc":
                case "rnc":
                case "opc":
                    appendCmd(cmd,"");
                    ClientFileManager.cmdController(rootPath, cmd, gui.getChatArea(),
                            formatter.format(date), network.getCurrentChannel(), future->{});
                    break;
                case "ops":

                    break;
                case "exit":
                    network.getCurrentChannel().writeAndFlush(Unpooled.copiedBuffer(splitedCmd[0].getBytes()));
                    gui.exit();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendCmd(String cmd, String attachment) {
        gui.getChatArea().append(formatter.format(date) + ": " + cmd + attachment + "\n");
    }

    public class GUI extends JFrame{
        private JTextField text;
        private JTextArea chatArea;

        public JTextField getText() {
            return text;
        }

        public void exit(){this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));}

        public JTextArea getChatArea() {
            return chatArea;
        }

        public GUI() {
            Color chatBack = new Color(43,43,43);
            Color chatBack2 = new Color(60,63,65);
            Color textColor = new Color(177,186,177);
            Color sendColor = new Color(62,134,160);
            Color clearColor = new Color(199,84,80);

            setSize(400, 500);
            setTitle("FileTransfer");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setLocation(500, 250);

//        this.setUndecorated(true);
////        this.getContentPane().setBackground(chatBack);
//        this.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);


            // Текстовое поле для вывода сообщений
            chatArea = new JTextArea();
            chatArea.setEditable(false);
            chatArea.setLineWrap(true);
            chatArea.setBackground(chatBack2);
            chatArea.setForeground(sendColor);
            add(new JScrollPane(chatArea), BorderLayout.CENTER);
            chatArea.append("----------------------------------------\n" +
                    "Type <help> for command list\n" +
                    "----------------------------------------\n");

            // Нижняя панель с полем для ввода сообщений и кнопкой отправки сообщений
            JPanel bottomPanel = new JPanel(new BorderLayout());
            JButton send = new JButton("SEND");
            JButton clear = new JButton("clear");
            send.setBackground(sendColor);
            clear.setBackground(clearColor);
            bottomPanel.add(send, BorderLayout.WEST);
            bottomPanel.add(clear, BorderLayout.EAST);
            add(bottomPanel, BorderLayout.SOUTH);
            text = new JTextField();
            text.setForeground(textColor);
            text.setBackground(chatBack2);
            bottomPanel.add(text, BorderLayout.CENTER);
            text.setCaretColor(Color.green);


            text.addActionListener(a->{
                cmd = text.getText();
                splitedCmd = cmd.split(" ");

                commandController();

                text.setText("");
                text.grabFocus();
            });
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
}
