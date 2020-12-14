import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;
import java.nio.file.Paths;

public class Client extends JFrame {

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;

    public Client() throws HeadlessException, IOException {
        socket = new Socket("localhost", 8189);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        createGUI();
    }

    private void createGUI(){
        setSize(300, 300);
        setTitle("FileTransfer");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocation(500,250);

        JButton send = new JButton("SEND");
        JTextField text = new JTextField();
        add(text, BorderLayout.CENTER);
        add(send, BorderLayout.SOUTH);
        text.setCaretColor(Color.green);

        send.addActionListener(a -> {
            String[] cmd = text.getText().split(" ");
            if (cmd[0].equals("upload")) {
                sendFile(cmd[1]);
            }
            if (cmd[0].equals("download")) {
                getFile(cmd[1]);
            }
            text.setText("");
            text.grabFocus();
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosed(e);
                sendMessage("exit");
            }
        });
        setVisible(true);
    }

    private void getFile(String fileName) {
        try{
            out.writeUTF("download");
            out.writeUTF(fileName);
            File file = new File("client" + File.separator +"root_folder" + File.separator + fileName);
            if (file.exists()) { file.delete();}
            file.createNewFile();
            long size = in.readLong();
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[256];
            for (int i = 0; i < (size + 255) / 256; i++) {
                int read = in.read(buffer);
                fos.write(buffer, 0, read);
            }
            System.out.println(in.readUTF());
            fos.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void sendFile(String fileName) {
        try {
            out.writeUTF("upload");
            out.writeUTF(fileName);
            File file = new File("client" + File.separator +"root_folder" + File.separator + fileName);
            if (!file.exists()) {
                System.out.println("File is not exist");
                out.writeUTF("File is not exist");
            }
            long length = file.length();
            out.writeLong(length);
            FileInputStream fileBytes = new FileInputStream(file);
            int read = 0;
            byte[] buffer = new byte[256];
            while ((read = fileBytes.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            System.out.println(in.readUTF());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String text) {
        try {
            out.writeUTF(text);
            System.out.println(in.readUTF());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        new Client();
    }
}