package com.fbp.engine;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.core.rule.RuleExpression;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.external.ModbusWriterNode;
import com.fbp.engine.node.external.MqttPublisherNode;
import com.fbp.engine.node.external.MqttSubscriberNode;
import com.fbp.engine.node.internal.LogNode;
import com.fbp.engine.node.internal.RuleNode;
import com.fbp.engine.protocol.modbus.ModbusTcpSimulator;

import java.net.InetAddress;
import java.util.Map;
import java.util.function.Predicate;

public class Main_14_4 {
    public static void main(String[] args) throws InterruptedException {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(502, 10);

        FlowEngine flowEngine = new FlowEngine();

        Flow flow = new Flow("flow");

        MqttSubscriberNode mqttSubscriberNode = new MqttSubscriberNode("subscriber",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "client-sub",
                        "topic", "temperature",
                        "qos", 1));

        RuleNode ruleNode = new RuleNode("ruler", new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression ruleExpression = new RuleExpression("temperature", ">", 30.0);
                return ruleExpression.evaluate(message);
            }
        });

        MqttPublisherNode mqttPublisherNode = new MqttPublisherNode("publisher",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "client-pub",
                        "topic", "alert-temperature",
                        "qos", 1));

        LogNode logNode = new LogNode("logger");

        ModbusWriterNode modbusWriterNode = new ModbusWriterNode("writer",
                Map.of("host", "localhost",
                        "port", 502,
                        "slaveId", 1,
                        "registerAddress", 2,
                        "valueField", "temperature"));

        flow.addNode(mqttSubscriberNode)
                .addNode(ruleNode)
                .addNode(mqttPublisherNode)
                .addNode(modbusWriterNode)
                .addNode(logNode)
                .connect(mqttSubscriberNode.getId(), "out", ruleNode.getId(), "in")
                .connect(ruleNode.getId(), "match", mqttPublisherNode.getId(), "in")
                .connect(ruleNode.getId(), "match", modbusWriterNode.getId(), "in")
                .connect(ruleNode.getId(), "mismatch", logNode.getId(), "in");

        flowEngine.register(flow);

        simulator.start();
        flowEngine.startFlow("flow");

//        Thread.sleep(10000);
//        flowEngine.shutdown();
//        simulator.stop();
    }
}
