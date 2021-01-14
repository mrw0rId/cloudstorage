package ru.geekbrains.cloudstorage.util;

import ru.geekbrains.cloudstorage.NettyClient;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GUI extends JFrame {
    private JTextField text;
    private JTextArea chatArea;


    public void exit(){this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));}

    public JTextArea getChatArea() {
        return chatArea;
    }

    public GUI(NettyClient nettyClient) {
        Color chatBack = new Color(43,43,43);
        Color chatBack2 = new Color(60,63,65);
        Color textColor = new Color(177,186,177);
        Color textColor2 = new Color(183,118,48);
        Color sendColor = new Color(62,134,160);
        Color clearColor = new Color(199,84,80);

        //Окно
        setSize(450, 600);
        setTitle("FileTransfer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocation(500, 200);

        // Текстовое поле для вывода сообщений
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setBackground(chatBack2);
        chatArea.setForeground(textColor2);
        add(new JScrollPane(chatArea), BorderLayout.CENTER);
        chatArea.append("----------------------------------------\n" +
                "Type <help> for command list\n" +
                "----------------------------------------\n");

        // Нижняя панель с полем для ввода сообщений и кнопкой отправки сообщений
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JButton send = new JButton("SEND");
        JButton clear = new JButton("clear");
        text = new JTextField();

        send.setBackground(sendColor);
        clear.setBackground(clearColor);
        text.setForeground(textColor);
        text.setBackground(chatBack2);
        text.setCaretColor(Color.green);

        bottomPanel.add(send, BorderLayout.WEST);
        bottomPanel.add(clear, BorderLayout.EAST);
        bottomPanel.add(text, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        //Слушатели полей
        text.addActionListener(a->{
            nettyClient.setCmd(text.getText());
            nettyClient.setSplitedCmd(nettyClient.getCmd().split(" "));
            nettyClient.commandController();

            text.setText("");
            text.grabFocus();
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
        send.addActionListener(a -> {
            nettyClient.setCmd(text.getText());
            nettyClient.setSplitedCmd(nettyClient.getCmd().split(" "));
            nettyClient.commandController();

            text.setText("");
            text.grabFocus();
            chatArea.setCaretPosition(chatArea.getDocument().getLength());
        });
        clear.addActionListener(a -> {
            chatArea.setText("----------------------------------------\n" +
                    "Type <help> for command list\n" +
                    "----------------------------------------\n");
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                nettyClient.getNetwork().stop();
                super.windowClosed(e);
            }
        });
        setVisible(true);
    }
}
