package com.fbp.engine.node.external;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.protocol.modbus.ModbusTcpSimulator;
import org.junit.jupiter.api.*;

import java.net.InetAddress;
import java.util.Map;

public class ModbusReaderNodeTest {
    ModbusReaderNode readerNode;
    ModbusTcpSimulator simulator;

    Map<String,Object> readerConfig;
    int port;

    @BeforeEach
    void setUp(){
        port = RandomUtil.getRandomServerPort();
        simulator = new ModbusTcpSimulator(port, 10);
        simulator.setRegister(0, 38);
        simulator.setRegister(2,80);
        readerConfig = Map.of(
                "host", InetAddress.getLoopbackAddress().getHostAddress(),
                "port", port,
                "slaveId", 1001,
                "startAddress", 0,
                "count", 3,
                "registerMapping", Map.of(
                        "temperature", Map.of("index", 0),
                        "humidity", Map.of("index",1),
                        "control", Map.of("index", 2)
                )
        );
        readerNode = new ModbusReaderNode("reader", readerConfig);
    }

    @Order(1)
    @Test
    @DisplayName("포트 구성")
    void checkPortConfiguration(){
        Assertions.assertAll(
                ()->Assertions.assertNotNull(readerNode.getInputPort("trigger")),
                ()->Assertions.assertNotNull(readerNode.getOutputPort("out")),
                ()->Assertions.assertNotNull(readerNode.getOutputPort("error"))
        );
    }

    @Order(2)
    @Test
    @DisplayName("초기 상태")
    void checkInitState(){
        Assertions.assertFalse(readerNode.isConnected());
    }

    @Order(3)
    @Test
    @DisplayName("config 확인")
    void checkConfig(){
        Assertions.assertAll(
                ()->Assertions.assertEquals(readerConfig.get("host"), readerNode.getConfig("host")),
                ()->Assertions.assertEquals(readerConfig.get("port"), readerNode.getConfig("port")),
                ()->Assertions.assertEquals(readerConfig.get("slaveId"), readerNode.getConfig("slaveId")),
                ()->Assertions.assertEquals(readerConfig.get("startAddress"), readerNode.getConfig("startAddress")),
                ()->Assertions.assertEquals(readerConfig.get("count"), readerNode.getConfig("count")),
                ()->Assertions.assertEquals(readerConfig.get("registerMapping"),readerNode.getConfig("registerMapping"))
        );
    }

    @Order(4)
    @Test
    @DisplayName("연결 성공")
    void connectSuccess() throws Exception {
        simulator.start();
        readerNode.initialize();

        Thread.sleep(300);
        Assertions.assertTrue(readerNode.isConnected());

        readerNode.shutdown();
        simulator.stop();
    }

    @Order(5)
    @Test
    @DisplayName("레지스터 읽기")
    void checkRegisterRead() throws InterruptedException {
        simulator.start();
        readerNode.initialize();

        Thread.sleep(300);

        Connection connection = new Connection("conn-in");
        connection.setTarget(readerNode.getInputPort("trigger"));
        Connection connection1 = new Connection("conn-out");
        readerNode.getOutputPort("out").connect(connection1);

        connection.deliver(new Message(Map.of("t","r")));
        connection.poll();

        Message poll = connection1.poll();
        Assertions.assertAll(
                ()->Assertions.assertTrue(poll.toString().contains("temperature")),
                ()->Assertions.assertTrue(poll.toString().contains("38"))
        );

        readerNode.shutdown();
        simulator.stop();
    }

    @Order(6)
    @Test
    @DisplayName("registerMapping 적용")
    void checkRegisterMapping() throws InterruptedException {
        simulator.start();
        readerNode.initialize();

        Thread.sleep(300);

        Connection connection = new Connection("conn-in");
        connection.setTarget(readerNode.getInputPort("trigger"));
        Connection connection1 = new Connection("conn-out");
        readerNode.getOutputPort("out").connect(connection1);

        connection.deliver(new Message(Map.of("t","r")));
        connection.poll();

        Message poll = connection1.poll();
        Assertions.assertAll(
                ()->Assertions.assertTrue(poll.toString().contains("temperature")),
                ()->Assertions.assertTrue(poll.toString().contains("38")),
                ()->Assertions.assertTrue(poll.toString().contains("humidity")),
                ()->Assertions.assertTrue(poll.toString().contains("80"))
        );

        readerNode.shutdown();
        simulator.stop();
    }

    @Order(7)
    @Test
    @DisplayName("읽기 실패 시 에러 포트")
    void ifReadFailedThenSendErrorMsg() throws InterruptedException {
        Map<String, Object> readerConfig2 = Map.of(
                "host", InetAddress.getLoopbackAddress().getHostAddress(),
                "port", port,
                "slaveId", 1001,
                "startAddress", 0,
                "count", 99,
                "registerMapping", Map.of(
                        "temperature", Map.of("index", 0),
                        "humidity", Map.of("index",1),
                        "control", Map.of("index", 2)
                )
        );
        ModbusReaderNode readerNode2 = new ModbusReaderNode("reader", readerConfig2);

        simulator.start();
        readerNode2.initialize();

        Thread.sleep(300);

        Connection connection = new Connection("conn-in");
        connection.setTarget(readerNode2.getInputPort("trigger"));
        Connection connection1 = new Connection("conn-out");
        readerNode2.getOutputPort("error").connect(connection1);

        connection.deliver(new Message(Map.of("t","r")));
        connection.poll();

        Message poll = connection1.poll();
        Assertions.assertAll(
                ()->Assertions.assertNotNull(poll)
        );

        readerNode2.shutdown();
        simulator.stop();
    }

    @Order(8)
    @Test
    @DisplayName("shutdown 후 연결 해제")
    void ifShutdownThenDisconnect(){
        Assertions.assertFalse(readerNode.isConnected());

        readerNode.initialize();
        Assertions.assertTrue(readerNode.isConnected());

        readerNode.shutdown();
        Assertions.assertFalse(readerNode.isConnected());
    }
}
