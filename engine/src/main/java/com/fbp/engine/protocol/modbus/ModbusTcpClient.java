package com.fbp.engine.protocol.modbus;

import com.fbp.engine.protocol.modbus.exception.ModbusException;
import lombok.Getter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ModbusTcpClient {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String host;
    private int port;

    private AtomicInteger transactionId = new AtomicInteger(0);

    public ModbusTcpClient(String host, int port) {
        if(host==null){
            throw new IllegalArgumentException("host must be notNull");
        }
        if(port<0){
            throw new IllegalArgumentException("port must be more than or equal to 0");
        }
        this.host = host;
        this.port = port;
    }

    public void connect(){
        try {
            this.socket = new Socket(host,port);
            this.socket.setSoTimeout(3000);
            this.out = new DataOutputStream(socket.getOutputStream());
            this.in = new DataInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void disconnect(){
        if(socket !=null && !socket.isClosed()){
            try {
                socket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public boolean isConnected(){
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    /*
       ① FC 03 요청 프레임 조립 (MBAP 헤더 + PDU)
       ② 소켓으로 전송
       ③ 응답 프레임 수신 및 파싱
       ④ 에러 응답이면 ModbusException 발생
       ⑤ int[] 배열로 레지스터 값 반환
       */
    public int[] readHoldingRegisters(int unitId, int startAddress, int quantity) throws IOException, ModbusException {
        int sentTxId = transactionId.get();

        byte[] frame = buildReadHoldingRegistersFrame(unitId, startAddress, quantity);
        out.write(frame);

        Map<String, Integer> mbapHeader = readMbapHeader();
        if (mbapHeader.get("respTransactionId").intValue() != sentTxId) {
            throw new ModbusException(
                    String.format("Transaction ID mismatch. sent: %d, received: %d", sentTxId, mbapHeader.get("respTransactionId"))
            );
        }

        int respFunctionCode = in.readUnsignedByte();
        if (respFunctionCode == 0x83) {
            int exceptionCode = in.readUnsignedByte();
            throw new ModbusException(respFunctionCode, exceptionCode);
        }

        int byteCount = in.readUnsignedByte();
        int[] registers = new int[byteCount / 2];

        for (int i = 0; i < registers.length; i++) {
            registers[i] = in.readUnsignedShort();
        }
        return registers;
    }

    /*
        ① FC 06 요청 프레임 조립
        ② 소켓으로 전송
        ③ 응답 프레임 수신
        ④ 에코백 검증 (주소, 값 일치 확인)
        ⑤ 불일치 시 ModbusException 발생
         */
    public void writeSingleRegister(int unitId, int address, int value) throws IOException, ModbusException{
        int sentTxId = transactionId.get();

        byte[] frame = buildWriteSingleRegisterFrame(unitId, address, value);
        out.write(frame);
        out.flush();

        Map<String, Integer> mbapHeader = readMbapHeader();
        if (mbapHeader.get("respTransactionId").intValue() != sentTxId) {
            throw new ModbusException(
                    String.format("Transaction ID mismatch. sent: %d, received: %d", sentTxId, mbapHeader.get("respTransactionId"))
            );
        }

        int respFunctionCode = in.readUnsignedByte();
        if (respFunctionCode == 0x86) {
            int exceptionCode = in.readUnsignedByte();
            throw new ModbusException(respFunctionCode, exceptionCode);
        }

        int respAddress = in.readUnsignedShort();
        int respValue = in.readUnsignedShort();
        if (respAddress != address || respValue != value) {
            throw new ModbusException("Echo back mismatch");
        }
    }

    byte[] buildReadHoldingRegistersFrame(int unitId, int startAddress, int quantity) {
        int sentTxId = transactionId.getAndIncrement() & 0xFFFF;
        ByteBuffer buf = ByteBuffer.allocate(12);
        buf.put(buildMbapHeader(sentTxId, 6, unitId))
                .put((byte) 3)
                .putShort((short) startAddress)
                .putShort((short) quantity);
        return buf.array();
    }

    byte[] buildWriteSingleRegisterFrame(int unitId, int address, int value){
        ByteBuffer buffer = ByteBuffer.allocate(12);

        int sentTxId = transactionId.getAndIncrement() & 0xFFFF;
        buffer.put(buildMbapHeader(
                        sentTxId,
                        6,
                        unitId))
                .put((byte) 6)
                .putShort((short) address)
                .putShort((short) value);

        return buffer.array();
    }



    byte[] buildMbapHeader(int transactionId, int length, int unitId){
        ByteBuffer buffer = ByteBuffer.allocate(7);

        buffer.putShort((short) (transactionId & 0xFFFF))
                .putShort((short) 0)
                .putShort((short) length)
                .put((byte) unitId);

        return buffer.array();
    }

    Map<String, Integer> readMbapHeader() throws IOException {
        Map<String, Integer> parsedMap = new HashMap<>();
        parsedMap.put("respTransactionId", in.readUnsignedShort());
        parsedMap.put("respProtocolId", in.readUnsignedShort());
        parsedMap.put("respLength", in.readUnsignedShort());
        parsedMap.put("respUnitId", in.readUnsignedByte());
        return parsedMap;
    }

    public int getTransactionId(){
        return transactionId.get();
    }
}
