package com.fbp.engine.node.external;

import ch.qos.logback.classic.encoder.JsonEncoder;
import com.fbp.engine.core.node.ProtocolNode;
import com.fbp.engine.message.Message;
import com.fbp.engine.message.MessageListener;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;

@Slf4j
public class EchoProtocolNode extends ProtocolNode implements MessageListener {
    private InetAddress serverIp;
    private int serverPort;
    private Socket socketToServer;

    private PrintWriter toServerWriter;
    private BufferedReader fromServerReader;


    public EchoProtocolNode(String id, Map<String, Object> config, InetAddress serverIp, int serverPort) {
        super(id, config);
        if(serverIp==null){
            throw new IllegalArgumentException("serverIp must be notNull");
        }
        if(serverPort<0){
            throw new IllegalArgumentException("serverPort must be more than or equal to 0");
        }
        addInputPort("in");
        this.serverIp=serverIp;
        this.serverPort=serverPort;
    }

    @Override
    protected void connect() throws Exception{
        try {
            socketToServer = new Socket(serverIp,serverPort);
            toServerWriter = new PrintWriter(socketToServer.getOutputStream());
            fromServerReader = new BufferedReader(new InputStreamReader(socketToServer.getInputStream()));
        } catch (IOException e) {
            log.error("{}", e.getMessage(), e);
            throw new RuntimeException();
        }

        Thread t = new Thread(() -> {
            try {
                String line;
                while ((line = fromServerReader.readLine()) != null) {
                    onMessage(line);
                }
            } catch (IOException e) {
                log.error("{}", e.getMessage(), e);
                throw new RuntimeException();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @Override
    protected void disconnect(){
        if(socketToServer!=null && socketToServer.isConnected()){
            try {
                socketToServer.close();
            } catch (IOException e) {
                log.error("{}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void onProcess(String portName, Message message) {
        toServerWriter.write(message.toJson()+"\n");
        toServerWriter.flush();
    }

    @Override
    public void onMessage(String json) {
        Message message = Message.fromJson(json);
        System.out.println(message);

    }
}
