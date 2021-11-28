package com.example.ftpclient;

import org.apache.log4j.Logger;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Locale;

public class FtpPassive {
//    Logger logger = Logger.getLogger(FtpPassive.class);

    private BufferedReader controlReader;
    private PrintWriter controlOut;
    private String passHost;
    private int passPort;
    private String username;
    private String password;
    private String response;
    private boolean isLogin = false;
    private boolean isPassMode = false;
    private Socket socket;
    public enum TransferType {
        ASCII, BINARY
    };
    private TransferType type = TransferType.BINARY;
    public enum TransferMode {
        BLOCK,STREAM,ZIP
    }
    private TransferMode mode = TransferMode.STREAM;


    private static final int PORT = 21;

    public FtpPassive(String url,String username,String password){
        try {
            this.socket = new Socket(url,PORT);
            this.username = username;
            this.password = password;
            this.controlReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.controlOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);
            Init();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void Init() throws IOException {
        String msg = "";
        do {
            msg = controlReader.readLine();
//            logger.debug(msg);
        }while (!msg.startsWith("220 "));
    }

    public int login() throws Exception {
//        logger.debug(this.username);
        this.controlOut.println("USER "+this.username);

        this.response = this.controlReader.readLine();
//        System.out.println(response);
//        logger.debug(response);
        if (!this.response.startsWith("331 ")){//验证成功
//             throw new IOException(""+response);
//            logger.error(response);
            return -1;
        }

        controlOut.println("PASS "+ password);

        response = controlReader.readLine();
//        logger.debug(response);
        if (!response.startsWith("230 ")){
//            throw new IOException(""+response);
//            logger.error(response);
            return -1;
        }
        isLogin = true;
        return 1;
    }

    public int loginAnonymous() throws IOException {
        //        logger.debug(this.username);
        this.controlOut.println("USER ANONYMOUS");

        this.response = this.controlReader.readLine();
//        System.out.println(response);
//        logger.debug(response);
        if (!this.response.startsWith("331 ")){//验证成功
//             throw new IOException(""+response);
//            logger.error(response);
            return -1;
        }

        controlOut.println("PASS "+ password);

        response = controlReader.readLine();
//        logger.debug(response);
        if (!response.startsWith("230 ")){
//            throw new IOException(""+response);
//            logger.error(response);
            return -1;
        }
        isLogin = true;
        return 1;
    }

    public int logout() throws IOException {
        controlOut.println("QUIT");
        this.response = this.controlReader.readLine();
        if (!response.startsWith("221")){
            throw new IOException("Close connection failed!");
        }
//        logger.debug(response);
        this.isLogin = false;
        socket.close();
        if (socket.isClosed()){
//            System.out.println("关闭连接");
//            logger.debug("关闭连接");
            return 1;
        }
        return -1;
    }

    private void checkPassiveConnect() throws IOException {
        //是不是每次都要关服务器开放的端口
        if (!this.isPassMode){
            this.controlOut.println("PASV mode");
            response = this.controlReader.readLine();
//            logger.debug(response);
            if (!response.startsWith("271")){
                throw new IOException("FTPClient could not request passive mode: " + response);
            }
            int tempPort = Integer.parseInt(response.split(" ")[4]);
//            logger.debug("端口号："+tempPort);
            this.passHost = "127.0.0.1";
            this.passPort = tempPort;
            isPassMode = true;
        }
    }

    public int upload(String path) throws IOException {
//        logger.debug("File path: "+path);
        File file = new File(path);
        if (!file.exists()) {
//            logger.debug("File not exists...");
            return -1;
        }else {
            checkPassiveConnect();

            this.controlOut.println("STOR "+file.getName());
            Socket dataSocket = new Socket(this.passHost,this.passPort);
            response = this.controlReader.readLine();
//            logger.debug(response);
            FileInputStream inputStream = new FileInputStream(file);
            int bytesRead;
            if (this.type == TransferType.BINARY) {
                BufferedInputStream input = new BufferedInputStream(inputStream);
                BufferedOutputStream output = new BufferedOutputStream(dataSocket.getOutputStream());
                byte[] buffer = new byte[4096];
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.flush();
                input.close();
                output.close();
            }else {
                InputStreamReader input = new InputStreamReader(inputStream);
                OutputStreamWriter output = new OutputStreamWriter(dataSocket.getOutputStream());
                char[] buffer = new char[2048];
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.flush();
                output.close();
                input.close();
            }
            inputStream.close();
            dataSocket.close();
            response = this.controlReader.readLine();
//            logger.debug(response);
            return 1;
        }
    }

    public void uploadFold(String path) throws IOException {
        File file1 = new File(path);
        if (file1.exists()){
            File[] files = file1.listFiles();
            if (files!= null) {
                for (File file : files) {
                    if (file.isDirectory()){
                        uploadFold(file.getPath());
                    }else {
                        upload(file.getPath());
                    }
                }
            }
        }
    }

    public int download(String filename, String path) throws IOException {
//         logger.debug(filename);
         checkPassiveConnect();
         //send RETR command
         this.controlOut.println("RETR "+filename);
         response = controlReader.readLine();
//         logger.debug(response);
         //send data connection
         Socket dataSocket = new Socket(this.passHost,this.passPort);
         //Read data from server
        int bytesRead = 0;
        if (this.type == TransferType.BINARY) {
            BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(new File(path, filename)));
            BufferedInputStream input = new BufferedInputStream(dataSocket.getInputStream());
            byte[] buffer = new byte[4096];
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            output.flush();
            output.close();
            input.close();
        }else {
            OutputStreamWriter output = new OutputStreamWriter(new FileOutputStream(new File(path,filename)));
            InputStreamReader input = new InputStreamReader(dataSocket.getInputStream());
            char[] buffer = new char[2048];
            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            output.flush();
            output.close();
            input.close();
        }
        dataSocket.close();
        response = controlReader.readLine();
//        logger.debug(response);
        return 1;
    }

    public void downloadFold(String filename, String path) throws Exception {
        File file = new File(path,filename);
        file.mkdir();
        //send RETR command
        controlOut.println("RETR " + filename);
        ArrayList<String> files = new ArrayList<>();
        ArrayList<String> folds = new ArrayList<>();
        String[] res;
        while ((response=controlReader.readLine()).startsWith("160")){
//            logger.debug(response);
            res = response.split(" ");
            if (res[2].equals("file")){
                files.add(res[1]);
            }else {
                folds.add(res[1]);
            }
        }
        if (files.size()>0){
            for (String name:files) {
                download(filename+File.separator+name,path);
            }
        }
        if (folds.size()>0){
            for (String fold:folds){
                downloadFold(filename+File.separator+fold,path);
            }
        }

    }

    public void selectType(String type) throws IOException {
        type=type.toUpperCase(Locale.ROOT);
        if (type.equals("ASCII")){
            this.type = TransferType.ASCII;
            controlOut.println("TYPE ASCII");
        }else if (type.equals("BINARY")){
            this.type = TransferType.BINARY;
            controlOut.println("TYPE BINARY");
        }
        response = controlReader.readLine();
        if (!response.startsWith("280")){
            throw new IOException("Failed to set type "+response);
        }
//        logger.debug(response);
    }

    public void selectMode(String mode) throws IOException {
        mode = mode.toUpperCase(Locale.ROOT);
        if (mode.equals("BLOCK")){
            this.mode = TransferMode.BLOCK;
        }else if (mode.equals("STREAM")){
            this.mode = TransferMode.STREAM;
        }else if (mode.equals("ZIP")){
            this.mode = TransferMode.ZIP;
        }
        controlOut.println("MODE "+mode);
        response = controlReader.readLine();
        if (!response.startsWith("290")){
            throw new IOException("Failed to set mode "+response);
        }
//        logger.debug(response);
    }
}
