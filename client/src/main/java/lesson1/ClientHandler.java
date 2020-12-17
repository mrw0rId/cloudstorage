package lesson1;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Paths;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());
            while (true) {
                String command = in.readUTF();
                if (command.equals("upload")) {
                    try {
                        String fileName = in.readUTF();
                        File file = new File("server" + File.separator +"root_folder" + File.separator + fileName);
                        if (file.exists()) { file.delete(); }
                        file.createNewFile();
                        long size = in.readLong();
                        FileOutputStream fos = new FileOutputStream(file);
                        byte[] buffer = new byte[256];
                        for (int i = 0; i < (size + 255) / 256; i++) {
                            int read = in.read(buffer);
                            fos.write(buffer, 0, read);
                        }
                        fos.close();
                        out.writeUTF(fileName + " uploaded");
                    } catch (IOException e) {
                        out.writeUTF("Uploading failed");
                    }
                }
                if (command.startsWith("download")) {
                    try{
                        String fileName = in.readUTF();
                        File file = new File("server" + File.separator +"root_folder" + File.separator + fileName);
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
                        out.writeUTF(fileName + " downloaded");
                        out.flush();
                        String status = in.readUTF();
                        System.out.println(status);
                    } catch (IOException e) {
                        out.writeUTF("Downloading failed");
                    }
                }
                if (command.equals("exit")) {
                    System.out.println("lesson1.Client disconnected correctly");
                    out.writeUTF("lesson1.Client disconnected ");
                    break;
                }
            }
        } catch (SocketException socketException) {
            System.out.println("lesson1.Client disconnected");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}