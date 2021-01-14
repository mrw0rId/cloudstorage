package ru.geekbrains.cloudstorage;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import ru.geekbrains.cloudstorage.handlers.ClientFileManager;
import ru.geekbrains.cloudstorage.util.AuthState;
import ru.geekbrains.cloudstorage.util.GUI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.CountDownLatch;

public class NettyClient {

    private AuthState authState = AuthState.NOTAUTH;

    private final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    private final Network network;
    private final GUI gui;
    private Path rootPath;
    private String cmd;
    private String[] splitedCmd;
    private final String serviceCmd = "TYPE:\n" +
            "<help> for list of service commands\n" +
            "<auth 'login' 'password'> for authorization\n" +
            "<ex> for commands example\n" +
            "<lss> for list of files on Server\n" +
            "<lsc> for list of files on Client\n" +
            "<download 'filename'> for getting file from Server\n" +
            "<upload 'filename'> for uploading file to Server\n" +
            "<rms 'fullpath/filename or filename'> for deleting file from Server\n" +
            "<rmc 'fullpath/filename or filename'> for deleting file from Client\n" +
            "<rnc 'fullpath/filename or filename newFileName'> for renaming file on Client\n" +
            "<ops 'fullpath/filename or filename'> for opening file to Server\n" +
            "<opc 'fullpath/filename or filename'> for opening file to Server\n" +
            "<cdc 'fullpath/filename or filename'> for creating folder on Client\n" +
            "<cfc 'fullpath/filename or filename'> for creating file on Client\n" +
            "<lo> for logout\n" +
            "<exit> for quit program\n";
    private final String example = "\n------Commands example-------\n" +
            "help\n" +
            "auth login password\n" +
            "download 1.txt\n" +
            "upload 1.txt\n" +
            "rmc 1.txt\n" +
            "rmc client\\root_folder\\username\\first\\1.txt\n" +
            "rmc 1.txt\n" +
            "rnc client\\root_folder\\username\\first\\1.txt\n" +
            "cdc client\\root_folder\\username\\first\\second\n" +
            "exit\n" +
            "----------------------------------------\n";

    public NettyClient() {
        CountDownLatch networkStarter = new CountDownLatch(1);
        this.network = new Network();
        gui = new GUI(this);
        new Thread(() -> network.start(networkStarter, gui, this)).start();
    }


    public void commandController() {
        switch (splitedCmd[0]) {
            case "help":
                gui.getChatArea().append(serviceCmd);
                break;
            case "exit":
                network.getCurrentChannel().writeAndFlush(Unpooled.copiedBuffer(splitedCmd[0].getBytes()));
                gui.exit();
                break;
            case "ex":
                gui.getChatArea().append(example);
                break;
        }
        if (authState == AuthState.NOTAUTH) {
            doAuth();
        } else {
            try {
                switch (splitedCmd[0]) {
                    case "rns":
                    case "rms":
                    case "lo":
                    case "download":
                    case "ops":
                    case "lss":
                    case "cds":
                        appendCmd(cmd, "");
                        network.getCurrentChannel().writeAndFlush(Unpooled.copiedBuffer(cmd.getBytes()));
                        break;
                    case "upload":
                        appendCmd(cmd, "");
                        ClientFileManager.cmdController(rootPath, cmd, gui.getChatArea(),
                                formatter.format(new Date()), network.getCurrentChannel(), future -> {
                                    if (!future.isSuccess()) {
                                        future.cause().printStackTrace();
                                    }
                                    if (future.isSuccess()) {
                                        appendCmd(splitedCmd[1], " uploaded successfully");
                                        System.out.println(formatter.format(new Date()) + ": " + splitedCmd[1] + " uploaded successfully\n");
                                    }
                                });
                        break;
                    case "lsc":
                        appendCmd(cmd, "");
                        gui.getChatArea().append("------LIST OF CLIENT FILES-------\n");
                        Files.walk(rootPath).forEach(s -> gui.getChatArea().append(s.toString() + "\n"));
                        gui.getChatArea().append("--------------------------------------------\n");
                        break;
                    case "rmc":
                    case "rnc":
                    case "opc":
                    case "cdc":
                    case "cfc":
                        appendCmd(cmd, "");
                        ClientFileManager.cmdController(rootPath, cmd, gui.getChatArea(),
                                formatter.format(new Date()), network.getCurrentChannel(), future -> {
                                });
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void doAuth() {
        if (splitedCmd[0].equals("auth")) {
            network.getCurrentChannel().writeAndFlush(Unpooled.copiedBuffer(cmd.getBytes()));
        } else appendCmd("Please authorize first: auth 'login' 'password'", "");
    }

    private void appendCmd(String cmd, String attachment) {
        gui.getChatArea().append(formatter.format(new Date()) + ": " + cmd + attachment + "\n");
    }

    public Network getNetwork() {
        return network;
    }

    public String[] getSplitedCmd() {
        return splitedCmd;
    }

    public String getCmd() {
        return cmd;
    }

    public void setSplitedCmd(String[] splitedCmd) {
        this.splitedCmd = splitedCmd;
    }

    public void setCmd(String cmd) {
        this.cmd = cmd;
    }

    public void setAuthState(AuthState authState) {
        this.authState = authState;
    }

    public Path getRootPath() {
        return rootPath;
    }

    public void setRootPath(Path rootPath) {
        this.rootPath = rootPath;
        if(!this.rootPath.toFile().exists()) this.rootPath.toFile().mkdirs();
    }
}
