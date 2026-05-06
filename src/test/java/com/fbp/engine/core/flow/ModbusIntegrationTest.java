package com.fbp.engine.core.flow;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.external.ModbusReaderNode;
import com.fbp.engine.node.external.ModbusWriterNode;
import com.fbp.engine.node.internal.CollectorNode;
import com.fbp.engine.protocol.modbus.ModbusTcpSimulator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.Map;
import java.util.Random;

public class ModbusIntegrationTest {
    @Order(1)
    @Test
    @DisplayName("Reader -> 레지스터 읽기")
    void checkModbusReader() throws InterruptedException {
        int randomServerPort = RandomUtil.getRandomServerPort();
        ModbusTcpSimulator modbusTcpSimulator = new ModbusTcpSimulator(randomServerPort, 30);

        ModbusReaderNode modbusReaderNode = new ModbusReaderNode("reader", Map.of(
                "host", InetAddress.getLoopbackAddress().getHostAddress(),
                "port", randomServerPort,
                "slaveId", 1001,
                "startAddress", 0,
                "count", 3,
                "registerMapping", Map.of(
                        "temperature", Map.of("index", 0),
                        "humidity", Map.of("index",1),
                        "control", Map.of("index", 2)
                )));

        CollectorNode collectorStub = new CollectorNode("collector-stub");

        Flow flow = new Flow("flow");
        flow.addNode(modbusReaderNode)
                .addNode(collectorStub)
                .connect(modbusReaderNode.getId(), "out", collectorStub.getId(), "in");

        modbusTcpSimulator.start();
        Thread.sleep(70);

        modbusTcpSimulator.setRegister(0, 36);
        modbusTcpSimulator.setRegister(1, 90);
        modbusTcpSimulator.setRegister(2, 1);

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.startFlow("flow");

        Thread.sleep(70);
        modbusReaderNode.process("in", new Message(Map.of("tmp","tmp")));

        Thread.sleep(70);

        System.out.println(collectorStub.getCollected().get(0));
        Assertions.assertTrue(collectorStub.getCollected().get(0).toString().contains("temperature=36"));
        Assertions.assertTrue(collectorStub.getCollected().get(0).toString().contains("humidity=90"));
        Assertions.assertTrue(collectorStub.getCollected().get(0).toString().contains("control=1"));

        flowEngine.shutdown();
        modbusTcpSimulator.stop();
    }

    @Order(2)
    @Test
    @DisplayName("Writer -> 레지스터 쓰기")
    void checkModbusWriter() throws InterruptedException {
        int randomServerPort = RandomUtil.getRandomServerPort();
        ModbusTcpSimulator modbusTcpSimulator = new ModbusTcpSimulator(randomServerPort, 30);
        modbusTcpSimulator.start();

        ModbusWriterNode modbusWriterNode = new ModbusWriterNode("writer", Map.of(
                "host", "localhost",
                "port", randomServerPort,
                "slaveId", 1,
                "registerAddress", 2,
                "valueField", "temperature"
        ));

        Flow flow = new Flow("flow");
        flow.addNode(modbusWriterNode);

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.startFlow("flow");

        modbusWriterNode.process("in", new Message(Map.of("temperature", 36)));

        Thread.sleep(80);

        Assertions.assertEquals(36.0, modbusTcpSimulator.getRegister(2));
    }

    @Order(3)
    @Test
    @DisplayName("Reader -> Writer 파이프라인")
    void testReaderToWriterPipeLine() throws InterruptedException {
        int randomServerPort = RandomUtil.getRandomServerPort();
        ModbusTcpSimulator modbusTcpSimulator = new ModbusTcpSimulator(randomServerPort, 30);
        modbusTcpSimulator.start();

        modbusTcpSimulator.setRegister(0, 444);

        ModbusReaderNode modbusReaderNode = new ModbusReaderNode("reader", Map.of(
                "host", InetAddress.getLoopbackAddress().getHostAddress(),
                "port", randomServerPort,
                "slaveId", 1001,
                "startAddress", 0,
                "count", 3,
                "registerMapping", Map.of(
                        "temperature", Map.of("index", 0),
                        "humidity", Map.of("index",1),
                        "control", Map.of("index", 2)
                )));

        ModbusWriterNode modbusWriterNode = new ModbusWriterNode("writer", Map.of(
                "host", "localhost",
                "port", randomServerPort,
                "slaveId", 1,
                "registerAddress", 2,
                "valueField", "temperature"
        ));

        Flow flow = new Flow("flow");
        flow.addNode(modbusReaderNode)
                .addNode(modbusWriterNode)
                .connect(modbusReaderNode.getId(),"out", modbusWriterNode.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.startFlow("flow");

        modbusReaderNode.process("in", new Message(Map.of("trigger", "trigger")));

        Thread.sleep(65);

        Assertions.assertEquals(444, modbusTcpSimulator.getRegister(2));

        flowEngine.shutdown();
        modbusTcpSimulator.stop();
    }

    @Order(4)
    @Test
    @DisplayName("연결 끊김 처리")
    void checkDisconnectedProcessing() throws InterruptedException {
        int randomServerPort = RandomUtil.getRandomServerPort();
        ModbusTcpSimulator modbusTcpSimulator = new ModbusTcpSimulator(randomServerPort, 30);
        modbusTcpSimulator.start();

        modbusTcpSimulator.setRegister(0, 444);

        ModbusReaderNode modbusReaderNode = new ModbusReaderNode("reader", Map.of(
                "host", InetAddress.getLoopbackAddress().getHostAddress(),
                "port", randomServerPort,
                "slaveId", 1001,
                "startAddress", 0,
                "count", 3,
                "registerMapping", Map.of(
                        "temperature", Map.of("index", 0),
                        "humidity", Map.of("index",1),
                        "control", Map.of("index", 2)
                )));

        CollectorNode errCollector = new CollectorNode("errorCollect");

        Flow flow = new Flow("flow");
        flow.addNode(modbusReaderNode)
                .addNode(errCollector)
                .connect(modbusReaderNode.getId(),"error", errCollector.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.startFlow("flow");

        modbusTcpSimulator.stop();

        Thread.sleep(500);
        modbusReaderNode.process("in", new Message(Map.of("trigger", "trigger")));

        Thread.sleep(500);

        Assertions.assertEquals(1, errCollector.getCollected().size());

        modbusTcpSimulator.stop();
        flowEngine.shutdown();
    }
}
