    package com.fbp.engine;

    import com.fbp.engine.network.client.TcpHelloFbpClient;
    import com.fbp.engine.network.server.TcpEchoServer;

    import java.net.InetAddress;

    public class Main_11_1 {
        public static void main(String[] args) {
            TcpEchoServer tcpEchoServer = new TcpEchoServer();
            tcpEchoServer.start();

            TcpHelloFbpClient tcpHelloFbpClient = new TcpHelloFbpClient(InetAddress.getLoopbackAddress(), tcpEchoServer.getPort());

            Thread clientThread = new Thread(()->{
                while(!Thread.currentThread().isInterrupted()) {
                    tcpHelloFbpClient.helloFbp();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
            clientThread.setDaemon(true);
            clientThread.start();

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
            }

            tcpEchoServer.stop();
        }
    }
