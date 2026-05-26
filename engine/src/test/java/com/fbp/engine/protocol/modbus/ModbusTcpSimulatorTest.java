package com.fbp.engine.protocol.modbus;

import ch.qos.logback.core.testUtil.RandomUtil;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class ModbusTcpSimulatorTest {
    ModbusTcpSimulator simulator;
    int serverPort;

    @BeforeEach
    void setUp(){
        serverPort = RandomUtil.getRandomServerPort();
        simulator = new ModbusTcpSimulator(serverPort,10);
        simulator.setRegister(0,100);
    }

    @Order(1)
    @Test
    @DisplayName("시작/종료")
    void checkStartAndStop(){
        simulator.start();
        Assertions.assertFalse(simulator.getServerSocket().isClosed());

        simulator.stop();
        Assertions.assertTrue(simulator.getServerSocket().isClosed());
    }

    @Order(2)
    @Test
    @DisplayName("레지스터 초기값")
    void checkInitRegisterValue(){
        simulator.start();

        Assertions.assertEquals(100, simulator.getRegister(0));

        simulator.stop();
    }

    @Order(3)
    @Test
    @DisplayName("FC 03 응답")
    void checkFc03Response() throws IOException, InterruptedException {

        simulator.start();

        try (Socket client = new Socket(InetAddress.getLoopbackAddress().getHostAddress(), serverPort)) {
            ByteBuffer buf = ByteBuffer.allocate(12);
            buf.putShort((short) 0)
                    .putShort((short) 0)
                    .putShort((short) 1)
                    .put((byte) 1001)
                    .put((byte) 3)
                    .putShort((short) 0)
                    .putShort((short) 1);
            client.getOutputStream().write(buf.array());

            Thread.sleep(500);

            byte[] bytes = client.getInputStream().readNBytes(11);

            Assertions.assertEquals(100, ByteBuffer.wrap(bytes, 9, 2).getShort());
        }

        simulator.stop();
    }

    @Order(4)
    @Test
    @DisplayName("FC 06 응답")
    void checkFc06Response() throws IOException, InterruptedException {
        simulator.start();

        try (Socket client = new Socket(InetAddress.getLoopbackAddress().getHostAddress(), serverPort)) {
            ByteBuffer buf = ByteBuffer.allocate(12);
            buf.putShort((short) 0)
                    .putShort((short) 0)
                    .putShort((short) 1)
                    .put((byte) 1001)
                    .put((byte) 6)
                    .putShort((short) 0)
                    .putShort((short) 500);
            client.getOutputStream().write(buf.array());
            Thread.sleep(500);

            Assertions.assertEquals(500,simulator.getRegister(0));

            byte[] bytes = client.getInputStream().readNBytes(12);
            Assertions.assertAll(
                    ()->Assertions.assertEquals(0, ByteBuffer.wrap(bytes,0,2).getShort()),
                    ()->Assertions.assertEquals(0, ByteBuffer.wrap(bytes,2,2).getShort()),
                    ()->Assertions.assertEquals(1, ByteBuffer.wrap(bytes,4,2).getShort()),
                    ()->Assertions.assertEquals((byte) 1001, bytes[6]),
                    ()->Assertions.assertEquals((byte) 6, bytes[7]),
                    ()->Assertions.assertEquals(0, ByteBuffer.wrap(bytes,8,2).getShort()),
                    ()->Assertions.assertEquals(500, ByteBuffer.wrap(bytes,10,2).getShort())
            );
        }

        simulator.stop();
    }

    @Order(5)
    @Test
    @DisplayName("잘못된 주소 에러 - FC03 / FC06 모두 Exception Code 0x02 응답")
    void checkInvalidAddressError() throws IOException, InterruptedException {
        simulator.start();

        try (Socket client = new Socket(InetAddress.getLoopbackAddress().getHostAddress(), serverPort)) {
            ByteBuffer req = ByteBuffer.allocate(12);
            req.putShort((short) 1)
                    .putShort((short) 0)
                    .putShort((short) 6)
                    .put((byte) 1)
                    .put((byte) 3)
                    .putShort((short) 9)
                    .putShort((short) 5);
            client.getOutputStream().write(req.array());

            Thread.sleep(200);

            byte[] resp = client.getInputStream().readNBytes(9);
            Assertions.assertAll(
                    () -> Assertions.assertEquals((byte) 0x83, resp[7], "FC 03 에러 코드"),
                    () -> Assertions.assertEquals((byte) 0x02, resp[8], "Exception Code 0x02 (Illegal Data Address)")
            );
        }

        try (Socket client = new Socket(InetAddress.getLoopbackAddress().getHostAddress(), serverPort)) {
            ByteBuffer req = ByteBuffer.allocate(12);
            req.putShort((short) 2)
                    .putShort((short) 0)
                    .putShort((short) 6)
                    .put((byte) 1)
                    .put((byte) 6)
                    .putShort((short) 15)
                    .putShort((short) 100);
            client.getOutputStream().write(req.array());

            Thread.sleep(200);

            byte[] resp = client.getInputStream().readNBytes(9);
            Assertions.assertAll(
                    () -> Assertions.assertEquals((byte) 0x86, resp[7], "FC 06 에러 코드"),
                    () -> Assertions.assertEquals((byte) 0x02, resp[8], "Exception Code 0x02 (Illegal Data Address)")
            );
        }

        simulator.stop();
    }

    @Order(6)
    @Test
    @DisplayName("다중 클라이언트 - 2개 클라이언트가 동시 접속하여 독립적으로 요청/응답")
    void checkMultipleClients() throws IOException, InterruptedException {
        simulator.setRegister(1, 200);
        simulator.start();

        int[] clientAResult = new int[1];
        int[] clientBResult = new int[1];
        Throwable[] errors = new Throwable[2];

        Thread clientA = new Thread(() -> {
            try (Socket socket = new Socket(InetAddress.getLoopbackAddress().getHostAddress(), serverPort)) {
                ByteBuffer req = ByteBuffer.allocate(12);
                req.putShort((short) 10)
                        .putShort((short) 0)
                        .putShort((short) 6)
                        .put((byte) 1)
                        .put((byte) 6)
                        .putShort((short) 0)
                        .putShort((short) 999);
                socket.getOutputStream().write(req.array());

                Thread.sleep(200);

                byte[] resp = socket.getInputStream().readNBytes(12);
                clientAResult[0] = ByteBuffer.wrap(resp, 10, 2).getShort() & 0xFFFF;
            } catch (Throwable t) {
                errors[0] = t;
            }
        });

        Thread clientB = new Thread(() -> {
            try (Socket socket = new Socket(InetAddress.getLoopbackAddress().getHostAddress(), serverPort)) {
                ByteBuffer req = ByteBuffer.allocate(12);
                req.putShort((short) 20)
                        .putShort((short) 0)
                        .putShort((short) 6)
                        .put((byte) 1)
                        .put((byte) 3)
                        .putShort((short) 1)
                        .putShort((short) 1);
                socket.getOutputStream().write(req.array());

                Thread.sleep(200);

                byte[] resp = socket.getInputStream().readNBytes(11);
                clientBResult[0] = ByteBuffer.wrap(resp, 9, 2).getShort() & 0xFFFF;
            } catch (Throwable t) {
                errors[1] = t;
            }
        });

        clientA.start();
        clientB.start();
        clientA.join(2000);
        clientB.join(2000);

        Assertions.assertAll(
                () -> Assertions.assertNull(errors[0], "클라이언트 A 예외 없음"),
                () -> Assertions.assertNull(errors[1], "클라이언트 B 예외 없음"),
                () -> Assertions.assertEquals(999, clientAResult[0], "클라이언트 A: FC06 에코백 값"),
                () -> Assertions.assertEquals(999, simulator.getRegister(0), "FC06 후 레지스터 실제 반영"),
                () -> Assertions.assertEquals(200, clientBResult[0], "클라이언트 B: FC03 읽기 값")
        );

        simulator.stop();
    }
}
