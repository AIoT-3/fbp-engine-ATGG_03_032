package com.fbp.engine.protocol.modbus;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class ModbusTcpSimulator {
    private final Set<Socket> activeClients = Collections.synchronizedSet(new HashSet<>());

    @Getter
    @Setter
    private ServerSocket serverSocket;
    private int[] registers;
    private volatile boolean running;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public ModbusTcpSimulator(int port, int registerCount){
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.registers = new int[registerCount];
        this.running = false;
    }

    public void start(){
        this.running = true;
        executorService.submit(this::acceptLoop);
    }

    public void stop(){
        this.running = false;

        if(serverSocket!=null && !serverSocket.isClosed()){
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.error("{}", e.getMessage(), e);
            }
        }

        synchronized (activeClients) {
            for (Socket s : activeClients) {
                try { s.close(); } catch (IOException e) { log.error("Error closing client socket", e); }
            }
            activeClients.clear();
        }

        executorService.shutdownNow();
    }

    protected void acceptLoop(){
        while(running && !Thread.currentThread().isInterrupted()) {
            try {
                Socket client = serverSocket.accept();
                if (client != null) {
                    activeClients.add(client);
                    executorService.submit(() -> {
                        try {
                            handleClient(client);
                        } finally {
                            activeClients.remove(client);
                            try { client.close(); } catch (IOException e) {}
                        }
                    });
                }
            } catch (IOException e) {
                if(running) {
                    log.error("{}", e.getMessage(), e);
                }
            }
        }
    }

    protected void handleClient(Socket socket){
        try(DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream())){

            while(!socket.isClosed()) {
                int transactionId = in.readUnsignedShort();
                int protocolId = in.readUnsignedShort();
                int length = in.readUnsignedShort();
                int unitId = in.readUnsignedByte();
                int functionCode = in.readUnsignedByte();

                if(functionCode==3){
                    processFc03(out, in, transactionId, protocolId, length, unitId, functionCode);
                }else if(functionCode==6){
                    processFc06(out, in, transactionId, protocolId, length, unitId, functionCode);
                }else{
                    sendErrorResponse(out, transactionId, protocolId, unitId, functionCode, (byte) 0x01);
                }
            }
        } catch (EOFException e) {
            log.info("Client disconnected");
        } catch (Exception e) {
            log.error("handleClient error", e);
        }
    }

    synchronized public void setRegister(int address, int value){
        if(address>=registers.length){
            throw new IllegalStateException(String.format("address must be less than %d", registers.length));
        }
        registers[address] = value;
    }

    synchronized public int getRegister(int address){
        if(address>=registers.length){
            throw new IllegalStateException(String.format("address must be less than %d", registers.length));
        }
        return registers[address];
    }

    protected void processFc03(DataOutputStream out, DataInputStream in,
                             int transactionId, int protocolId, int length,
                             int unitId, int functionCode) throws IOException {
        int startAddress = in.readUnsignedShort();
        int quantity = in.readUnsignedShort();

        if (quantity == 0 || quantity > 125) {
            sendErrorResponse(out, transactionId, protocolId, unitId, functionCode, (byte) 0x03);
            return;
        }
        if (startAddress + quantity > registers.length) {
            sendErrorResponse(out, transactionId, protocolId, unitId, functionCode, (byte) 0x02);
            return;
        }

        ByteBuffer byteBuffer = ByteBuffer.allocate(9 + quantity * 2);
        byteBuffer.putShort((short) transactionId)
                .putShort((short) protocolId)
                .putShort((short) (3 + quantity * 2))
                .put((byte) unitId)
                .put((byte) functionCode)
                .put((byte) (quantity * 2));

        for (int i = startAddress; i < startAddress + quantity; i++) {
            byteBuffer.putShort((short) getRegister(i));
        }

        out.write(byteBuffer.array());
        out.flush();
    }

    protected void processFc06(DataOutputStream out, DataInputStream in,
                             int transactionId, int protocolId, int length,
                             int unitId, int functionCode) throws IOException {
        int registerAddress = in.readUnsignedShort();
        int value = in.readUnsignedShort();
        if(registerAddress>=registers.length){
            sendErrorResponse(out, transactionId, protocolId, unitId, functionCode, (byte) 0x02);
            return;
        }
        setRegister(registerAddress, value);

        ByteBuffer byteBuffer = ByteBuffer.allocate(12);
        byteBuffer.putShort((short) transactionId)
                .putShort((short) protocolId)
                .putShort((short) length)
                .put((byte) unitId)
                .put((byte) functionCode)
                .putShort((short) registerAddress)
                .putShort((short) value);

        out.write(byteBuffer.array());
        out.flush();
    }

    private void sendErrorResponse(DataOutputStream out,
                                   int transactionId, int protocolId,
                                   int unitId, int functionCode,
                                   byte exceptionCode) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(9);
        buf.putShort((short) transactionId)
                .putShort((short) protocolId)
                .putShort((short) 3)
                .put((byte) unitId)
                .put((byte) (functionCode | 0x80))
                .put(exceptionCode);

        out.write(buf.array());
        out.flush();
    }

}
