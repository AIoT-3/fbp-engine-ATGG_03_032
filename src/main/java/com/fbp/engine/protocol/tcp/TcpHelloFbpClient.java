package com.fbp.engine.protocol.tcp;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

@Slf4j
public class TcpHelloFbpClient {
    InetAddress serverIp;
    int serverPort;
    Socket server;

    public TcpHelloFbpClient(InetAddress serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        try {
            this.server = new Socket(serverIp,serverPort);
        } catch (IOException e) {
            log.error("{}", e.getMessage(), e);
        }
    }

    public void helloFbp(){
        try {
            PrintWriter printWriter = new PrintWriter(server.getOutputStream());
            String message = "Hello FBP\n";
            System.out.print(">> " + message);
            printWriter.write(message);
            printWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(server.getInputStream()));
            String message = bufferedReader.readLine();
            System.out.println("<< " + message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}
