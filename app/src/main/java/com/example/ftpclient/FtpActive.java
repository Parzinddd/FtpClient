package com.example.ftpclient;

import lombok.Setter;
import org.apache.log4j.Logger;


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.IllegalFormatCodePointException;
import java.util.Locale;

@Setter
public class FtpActive {
//    Logger logger = Logger.getLogger(FtpActive.class);

    private Socket socket;
    private BufferedReader controlReader;
    private PrintWriter controlOut;

    private String username;
    private String password;

    private String response="";

    private static final int PORT = 21;

    public static boolean isLogin = false;

    public enum TransferType {
        ASCII, BINARY
    };

    public enum TransferMode {
        BLOCK,STREAM,ZIP
    };
    private TransferMode mode = TransferMode.STREAM;
    private TransferType type = TransferType.BINARY;

    public FtpActive(String url, String username, String password) {
        try {
            this.socket = new Socket(url,PORT);
            this.username =username;
            this.password = password;
            this.controlReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.controlOut = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);
            Init();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void Init() throws IOException {
        String msg = "";
        do {
            msg = controlReader.readLine();
//            logger.debug(msg);
        }while (!msg.startsWith("220 "));
        //220 连接成功
//        logger.debug("连接成功");
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
//         logger.debug(response);
         if (!this.response.startsWith("331 ")){//验证成功
//             throw new IOException(""+response);
//             logger.error(response);
             return -1;
         }

         controlOut.println("PASS "+ password);

         response = controlReader.readLine();
//         logger.debug(response);
         if (!response.startsWith("230 ")){
//            throw new IOException(""+response);
//             logger.error(response);
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
        socket.close();
        if (socket.isClosed()){
//            System.out.println("关闭连接");
//            logger.debug("关闭连接");
            return 1;
        }
        return -1;
    }

    public int port() throws IOException {
        String url = "127.0.0.1";
        int dataPort = (int)(Math.random()*100000%9999)+1024;//随机一个端口
        String portCommand = "MYPORT "+ url + "," + dataPort;
//        logger.debug(portCommand);
        controlOut.println(portCommand);
        //判断回复
        response = controlReader.readLine();
//        logger.debug(response);
        //Send command
        return  dataPort;
    }
    /**
     * \数据流传输
     * @param filepath
     */
    public void upload(String filepath) throws IOException {
//        logger.debug(filepath);
        File file = new File(filepath);
        if (!file.exists()){
//            logger.debug("File not Exists...");
        }else {
            InputStream inputStream = new FileInputStream(file);

            int dataPort = port();

            controlOut.println("STOR " + file.getName());
//            logger.debug(file.getName());
            //Open data connection
            ServerSocket dataSocketServer = new ServerSocket(dataPort);
            Socket dataSocket = dataSocketServer.accept();
            //Read command response
            response = controlReader.readLine();
//            logger.debug(response);
            //Read data from server


            int bytesRead = 0;
            if (type == TransferType.ASCII) {
                OutputStreamWriter output = new OutputStreamWriter(dataSocket.getOutputStream(), StandardCharsets.US_ASCII);
                char[] buffer = new char[2048];
                InputStreamReader input = new InputStreamReader(inputStream, StandardCharsets.US_ASCII);
                while ((bytesRead = input.read(buffer)) != -1){
                    output.write(buffer,0,bytesRead);
                }
                input.close();
                output.flush();
                output.close();
            }else {
                BufferedOutputStream output = new BufferedOutputStream(dataSocket.getOutputStream());
                BufferedInputStream input = new BufferedInputStream(inputStream);
                byte[] buffer = new byte[4096];
                while ((bytesRead = input.read(buffer)) != -1){
                    output.write(buffer,0,bytesRead);
                }
                input.close();
                output.flush();
                output.close();
            }

            dataSocket.close();
            //判断回复
            response = controlReader.readLine();
//            logger.debug(response);

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


    //TODO: 块传输
    //TODO: 压缩传输
    //download
    public void download(String filename, String path)throws Exception{//path 下载地址
            int dataPort = port();
            //send RETR command
            controlOut.println("RETR " + filename);
            //send data connection
            ServerSocket dataSocketServ = new ServerSocket(dataPort);
            Socket dataSocket = dataSocketServ.accept();
            //Read data from server
            int bytesRead = 0;
            if (type == TransferType.ASCII){
                File file = new File(path,filename);
                if (!file.exists()){
                    file.createNewFile();
                }
                OutputStream output = new FileOutputStream(file);
                OutputStreamWriter inFile = new OutputStreamWriter(output, StandardCharsets.US_ASCII);
                InputStreamReader input = new InputStreamReader(dataSocket.getInputStream(), StandardCharsets.US_ASCII);
                char[] buffer = new char[2048];
                while ((bytesRead = input.read(buffer)) != -1) {
                    inFile.write(buffer, 0, bytesRead);
                }
                inFile.flush();
                inFile.close();
                output.close();
                input.close();
            }else {
                RandomAccessFile inFile = new RandomAccessFile(path + File.separator + filename, "rw");
                BufferedInputStream input = new BufferedInputStream(dataSocket.getInputStream());
                byte[] buffer = new byte[4096];
                while ((bytesRead = input.read(buffer)) != -1) {
                    inFile.write(buffer, 0, bytesRead);
                }
                inFile.close();
                input.close();
            }
            dataSocket.close();
            //
            response = controlReader.readLine();
//            logger.debug(response);
            //
            response = controlReader.readLine();
//            logger.debug(response);
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
