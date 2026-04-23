package com.fbp.engine;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.node.external.ModbusReaderNode;
import com.fbp.engine.node.internal.PrintNode;
import com.fbp.engine.node.internal.TimerNode;
import com.fbp.engine.protocol.modbus.ModbusTcpSimulator;

import java.net.InetAddress;
import java.util.Map;


/*
config keys::
host(String),
port(int, 기본 502),
slaveId(int),
startAddress(int),
count(int),
registerMapping(Map<String, Object>, 선택)
 */
public class Main_13_8 {
    public static void main(String[] args) throws InterruptedException {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(502, 10);
        simulator.setRegister(0, 250);
        simulator.setRegister(1, 600);
        simulator.setRegister(2, 1);

        Map<String, Object> readerConfig = Map.of(
                "host", InetAddress.getLoopbackAddress().getHostAddress(),
                "port", 502,
                "slaveId", 1001,
                "startAddress", 0,
                "count", 10
        );

        TimerNode timerNode = new TimerNode("timer", 500);
        ModbusReaderNode modbusReaderNode = new ModbusReaderNode("modbus-reader", readerConfig);
        PrintNode printNode = new PrintNode("printer");

        Flow flow = new Flow("flow");
        flow.addNode(timerNode)
                .addNode(modbusReaderNode)
                .addNode(printNode)
                .connect(timerNode.getId(), "out", modbusReaderNode.getId(), "trigger")
                .connect(modbusReaderNode.getId(), "out", printNode.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);

        simulator.start();
        flowEngine.startFlow("flow");

        Thread.sleep(10000);

        flowEngine.shutdown();
        simulator.stop();
    }
}
