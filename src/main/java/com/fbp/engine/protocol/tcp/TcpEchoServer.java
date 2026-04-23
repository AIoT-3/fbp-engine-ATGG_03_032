package com.fbp.engine.protocol.tcp;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
public class TcpEchoServer {
    class ClientSocketAndMessage {
        @Getter
        private Socket client;
        @Getter
        private String message;

        public ClientSocketAndMessage(Socket client, String message) {
            this.client = Objects.requireNonNull(client, "client must be notNull");
            this.message = Objects.requireNonNull(message, "message must be notNull");
        }
    }

    @Getter
    private int port = -1;
    @Getter
    private ServerSocket serverSocket;
    private final BlockingQueue<ClientSocketAndMessage> messageQueue = new LinkedBlockingQueue<>();

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public TcpEchoServer() {
        try {
            this.serverSocket = new ServerSocket(0);
        } catch (IOException e) {
            log.error("{}", e.getMessage(), e);
        }
        this.port = serverSocket.getLocalPort();
    }

    public TcpEchoServer(int port) {
        if (port < 0) {
            throw new IllegalArgumentException("port must be more than or equal to 0");
        }
        this.port = port;

        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            log.error("{}", e.getMessage(), e);
        }
    }
    protected void acceptLoop(){
        while(!Thread.currentThread().isInterrupted()) {
            try {
                Socket client = serverSocket.accept();
                if (client != null) {
                    executorService.submit(() -> {
                        receiveLoop(client);
                    });
                }
            } catch (IOException e) {
                log.error("{}", e.getMessage(), e);
            }
        }
    }

    protected void receiveLoop(Socket client) {
        while(!Thread.currentThread().isInterrupted()) {
            try {
                BufferedReader bf = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String message = bf.readLine();
                messageQueue.put(new ClientSocketAndMessage(client, message));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                log.error("{}", e.getMessage(), e);
            }
        }
    }

    protected void dispatchLoop() {
        while(!Thread.currentThread().isInterrupted()) {
            try {
                ClientSocketAndMessage clientSocketAndMessage = messageQueue.take();
                Socket client = clientSocketAndMessage.getClient();
                PrintWriter printWriter = new PrintWriter(client.getOutputStream());
                printWriter.write(clientSocketAndMessage.getMessage() + "\n");
                printWriter.flush();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                log.error("{}", e.getMessage(), e);
            }
        }
    }

    public void start(){
        executorService.submit(this::acceptLoop);
        executorService.submit(this::dispatchLoop);
    }

    public void stop(){
        if(serverSocket!=null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.error("{}", e.getMessage(), e);
            }
        }
        executorService.shutdownNow();
    }
}