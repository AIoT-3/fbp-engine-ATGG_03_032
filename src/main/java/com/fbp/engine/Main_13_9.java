package com.fbp.engine;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.external.ModbusReaderNode;
import com.fbp.engine.node.external.ModbusWriterNode;
import com.fbp.engine.node.internal.ThresholdFilterNode;
import com.fbp.engine.protocol.modbus.ModbusTcpSimulator;

import java.net.InetAddress;
import java.util.Map;

public class Main_13_9 {
    public static void main(String[] args) throws InterruptedException {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(502, 10);
        simulator.setRegister(0, 38);

        Map<String, Object> readerConfig = Map.of(
                "host", InetAddress.getLoopbackAddress().getHostAddress(),
                "port", 502,
                "slaveId", 1001,
                "startAddress", 0,
                "count", 1,
                "registerMapping", Map.of(
                        "temperature", Map.of("index", 0),
                        "control", Map.of("index", 2)
                )
        );

        /*
host(String),
port(int, 기본 502),
slaveId(int),
registerAddress(int),
valueField(String — FBP Message에서 값을 읽을 키),
scale(double, 기본 1.0)
 */
        Map<String, Object> writerConfig = Map.of(
            "host", InetAddress.getLoopbackAddress().getHostAddress(),
                "port", 502,
                "slaveId", 1001,
                "registerAddress", 2,
                "valueField", "temperature"
        );

        ModbusReaderNode modbusReaderNode = new ModbusReaderNode("modbus-reader",readerConfig);
        ThresholdFilterNode thresholdFilterNode = new ThresholdFilterNode("filter", "temperature", 35.0);
        ModbusWriterNode modbusWriterNode = new ModbusWriterNode("modbus-writer", writerConfig);

        Flow flow = new Flow("flow");
        flow.addNode(modbusReaderNode)
                .addNode(thresholdFilterNode)
                .addNode(modbusWriterNode)
                .connect(modbusReaderNode.getId(),"out", thresholdFilterNode.getId(),"in")
                .connect(thresholdFilterNode.getId(),"alert", modbusWriterNode.getId(),"in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);

        simulator.start();
        flowEngine.startFlow("flow");

        Connection connection = new Connection("trigger");
        connection.setTarget(modbusReaderNode.getInputPort("trigger"));
        connection.deliver(new Message(Map.of("k","v")));
        connection.poll();
        Thread.sleep(100);

        System.out.println("register[2]=" + simulator.getRegister(2));

        flowEngine.shutdown();
        simulator.stop();
    }
}
