package com.fbp.engine.protocol.modbus;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.fbp.engine.protocol.modbus.exception.ModbusException;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Random;

public class ModbusTcpClientTest {
    ModbusTcpClient client;

    ModbusTcpSimulator simulator;

    @BeforeEach
    void setUp() throws UnknownHostException {
        int port = RandomUtil.getRandomServerPort();
        client = new ModbusTcpClient(InetAddress.getLoopbackAddress().getHostAddress(), port);

        simulator = new ModbusTcpSimulator(port, 10);
        simulator.setRegister(0, 250);
        simulator.setRegister(1, 600);
        simulator.setRegister(2, 1);
    }

    @Order(1)
    @Test
    @DisplayName("FC 03 요청 프레임 조립")
    void checkFc03RequestFrameBuild(){
        byte[] frame = client.buildReadHoldingRegistersFrame(1001, 3, 3);

        Assertions.assertAll(
                () -> Assertions.assertEquals(12, frame.length),
                () -> Assertions.assertEquals((byte) 1001, frame[6]),
                () -> Assertions.assertEquals((byte)  3, frame[7]),
                () -> Assertions.assertEquals(3, ByteBuffer.wrap(frame,8,2).getShort()),
                () -> Assertions.assertEquals(3, ByteBuffer.wrap(frame, 10, 2).getShort())
        );
    }

    @Order(2)
    @Test
    @DisplayName("FC 06 요청 프레임 조립")
    void checkFc06RequestFrameBuild(){
        byte[] frame = client.buildWriteSingleRegisterFrame(1001, 3, 38);

        Assertions.assertAll(
                () -> Assertions.assertEquals(12, frame.length),
                () -> Assertions.assertEquals((byte) 1001, frame[6]),
                () -> Assertions.assertEquals((byte)  6, frame[7]),
                () -> Assertions.assertEquals(3, ByteBuffer.wrap(frame,8,2).getShort()),
                () -> Assertions.assertEquals(38, ByteBuffer.wrap(frame,10,2).getShort())
        );
    }
    @Order(3)
    @Test
    @DisplayName("MBAP 헤더 구조")
    void checkMbapHeader(){
        byte[] frame = client.buildMbapHeader(1000, 5, 3);

        Assertions.assertAll(
                () -> Assertions.assertEquals(1000, ByteBuffer.wrap(frame, 0, 2).getShort()),
                () -> Assertions.assertEquals(0, ByteBuffer.wrap(frame,2,2).getShort()),
                () -> Assertions.assertEquals(5, ByteBuffer.wrap(frame,4,2).getShort()),
                () -> Assertions.assertEquals((byte) 3, frame[6])
        );
    }

    @Order(4)
    @Test
    @DisplayName("Transaction ID 증가")
    void checkTransactionIdIncrement(){
        for(int i=0; i<100; i++) {
            int startTransactionId = client.getTransactionId();
            client.buildWriteSingleRegisterFrame(0,0,0);
            Assertions.assertEquals(startTransactionId + 1, client.getTransactionId());
        }
    }

    @Order(5)
    @Test
    @DisplayName("초기 상태")
    void checkInitState(){
        Assertions.assertFalse(client.isConnected());
    }

    @Order(6)
    @Test
    @DisplayName("연결/해제")
    void checkConnectAndDisconnect(){
        simulator.start();

        Assertions.assertFalse(client.isConnected());
        client.connect();
        Assertions.assertTrue(client.isConnected());
        client.disconnect();
        Assertions.assertFalse(client.isConnected());

        simulator.stop();
    }

    @Order(7)
    @Test
    @DisplayName("Holding Register 읽기")
    void checkReadHoldingRegister() throws IOException, ModbusException {
        simulator.start();

        client.connect();
        int[] ints = client.readHoldingRegisters(0, 0, 1);
        Assertions.assertEquals(250, ints[0]);
        client.disconnect();

        simulator.stop();
    }

    @Order(8)
    @Test
    @DisplayName("다수 레지스터 읽기")
    void checkReadHoldingRegisters() throws IOException, ModbusException {
        simulator.start();

        client.connect();
        int[] ints = client.readHoldingRegisters(0, 0, 3);
        Assertions.assertAll(
                ()->Assertions.assertEquals(250, ints[0]),
                ()->Assertions.assertEquals(600, ints[1]),
                ()->Assertions.assertEquals(1,ints[2])
        );
        client.disconnect();

        simulator.stop();
    }

    @Order(9)
    @Test
    @DisplayName("Single Register 쓰기")
    void checkWriteSingleRegister() throws IOException, ModbusException {
        simulator.start();

        client.connect();
        client.writeSingleRegister(0,0,100);
        client.disconnect();

        Assertions.assertEquals(100, simulator.getRegister(0));

        simulator.stop();
    }

    @Order(10)
    @Test
    @DisplayName("쓰기 후 읽기")
    void readAfterWrite() throws IOException, ModbusException {
        simulator.start();

        client.connect();
        client.writeSingleRegister(0,0,100);
        int[] ints = client.readHoldingRegisters(0, 0, 1);
        Assertions.assertEquals(100, ints[0]);
        client.disconnect();

        simulator.stop();
    }

    @Order(11)
    @Test
    @DisplayName("에러 응답 처리")
    void errorResponse() throws IOException, ModbusException {
        simulator.start();

        client.connect();
        Assertions.assertThrows(ModbusException.class, ()-> {
                    try {
                        client.readHoldingRegisters(0, 0, 100);
                    } catch (ModbusException e) {
                        Assertions.assertEquals(ModbusException.ILLEGAL_DATA_ADDRESS, e.getExceptionCode());
                        throw e;
                    }
                }
        );
        client.disconnect();

        simulator.stop();
    }

    @Order(12)
    @Test
    @DisplayName("소켓 타임 아웃")
    void IfSimulatorStoppedThenRequestThrowsException() throws InterruptedException {
        simulator.start();
        client.connect();
        simulator.stop();
        Thread.sleep(500);

        Assertions.assertThrows(Exception.class, () ->{
            client.readHoldingRegisters(0,0,1);
        });
        client.disconnect();
    }






}
