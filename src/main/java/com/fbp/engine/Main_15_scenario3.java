package com.fbp.engine;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.core.rule.RuleExpression;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.external.ModbusWriterNode;
import com.fbp.engine.node.external.MqttPublisherNode;
import com.fbp.engine.node.external.MqttSubscriberNode;
import com.fbp.engine.node.internal.RuleNode;
import com.fbp.engine.protocol.modbus.ModbusTcpSimulator;

import java.net.InetAddress;
import java.util.Map;
import java.util.Random;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

public class Main_15_scenario3 {
    public static void main(String[] args) throws InterruptedException {
        int modbusPort = RandomUtil.getRandomServerPort();
        ModbusTcpSimulator modbusTcpSimulator = new ModbusTcpSimulator(modbusPort, 30);

        MqttPublisherNode scenarioStub = new MqttPublisherNode("stub",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-pub",
                        "topic", "sensor/temp",
                        "qos", 1,
                        "retained", false));

        MqttSubscriberNode mqttSubscriberNode = new MqttSubscriberNode("subscriber",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-sub",
                        "topic", "sensor/temp",
                        "qos", 1));

        RuleNode ruleNode = new RuleNode("ruler", new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression parse = RuleExpression.parse("value > 30");
                return parse.evaluate(message);
            }
        });

        ModbusWriterNode modbusWriterNode = new ModbusWriterNode("modbus-writer",
                Map.of(
                        "host", InetAddress.getLoopbackAddress().getHostAddress(),
                        "port", modbusPort,
                        "slaveId", 1,
                        "registerAddress", 2,
                        "valueField", "value"
                ));

        modbusTcpSimulator.start();
        Flow flow = new Flow("flow");

        flow.addNode(scenarioStub)
                .addNode(mqttSubscriberNode)
                .addNode(ruleNode)
                .addNode(modbusWriterNode)
                .connect(mqttSubscriberNode.getId(), "out", ruleNode.getId(), "in")
                .connect(ruleNode.getId(), "match", modbusWriterNode.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.startFlow("flow");

        while (true) {
            Thread.sleep(550);
            System.out.println("controll register value: " + modbusTcpSimulator.getRegister(2));
            scenarioStub.process("tmp", new Message(Map.of("value", RandomGenerator.getDefault().nextInt(20) + 20.0)));
            Thread.sleep(550);
        }
    }
}
