package com.fbp.engine;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.core.rule.RuleExpression;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.external.ModbusReaderNode;
import com.fbp.engine.node.external.ModbusWriterNode;
import com.fbp.engine.node.internal.RuleNode;
import com.fbp.engine.node.internal.TimerNode;
import com.fbp.engine.protocol.modbus.ModbusTcpSimulator;

import java.net.InetAddress;
import java.util.Map;
import java.util.function.Predicate;

public class Main_15_scenario2 {
    public static void main(String[] args) throws InterruptedException {
        int modbusPort = RandomUtil.getRandomServerPort();
        ModbusTcpSimulator modbusTcpSimulator = new ModbusTcpSimulator(modbusPort, 30);
        modbusTcpSimulator.setRegister(0, 36);

        TimerNode timerNode = new TimerNode("timer", 1000);

        ModbusReaderNode modbusReaderNode = new ModbusReaderNode("modbus-reader", Map.of(
                "host", InetAddress.getLoopbackAddress().getHostAddress(),
                "port", modbusPort,
                "slaveId", 1001,
                "startAddress", 0,
                "count", 3,
                "registerMapping", Map.of(
                        "temperature", Map.of("index", 0),
                        "humidity", Map.of("index",1),
                        "control", Map.of("index", 2)
                )));

        RuleNode ruleNode = new RuleNode("ruler", new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression ruleExpression = RuleExpression.parse("temperature > 30");
                return ruleExpression.evaluate(message);
            }
        });

        ModbusWriterNode modbusWriterNode = new ModbusWriterNode("modbus-writer",
                Map.of(
                        "host", InetAddress.getLoopbackAddress().getHostAddress(),
                        "port", modbusPort,
                        "slaveId", 1,
                        "registerAddress", 2,
                        "valueField", "temperature"
                ));

        modbusTcpSimulator.start();

        Flow flow =  new Flow("flow");
        flow.addNode(timerNode)
                .addNode(modbusReaderNode)
                .addNode(ruleNode)
                .addNode(modbusWriterNode)
                .connect(timerNode.getId(), "out", modbusReaderNode.getId(), "trigger")
                .connect(modbusReaderNode.getId(), "out", ruleNode.getId(), "in")
                .connect(ruleNode.getId(), "match", modbusWriterNode.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.startFlow("flow");

        Thread.sleep(200);
        while(true){
            System.out.println("controll register value: " + modbusTcpSimulator.getRegister(2));
            Thread.sleep(1000);
        }
    }
}
