package com.fbp.engine;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.core.rule.RuleExpression;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.external.MqttPublisherNode;
import com.fbp.engine.node.external.MqttSubscriberNode;
import com.fbp.engine.node.internal.RuleNode;
import com.fbp.engine.protocol.modbus.ModbusTcpSimulator;

import java.util.Map;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

public class Main_15_scenario1 {
    public static void main(String[] args) {
        MqttSubscriberNode mqttSubscriberNode = new MqttSubscriberNode("subscriber",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-sub",
                        "topic", "sensor/temp",
                        "qos", 1));

        RuleNode ruleNode = new RuleNode("ruler", new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression parse = RuleExpression.parse("value > 30.0");

                return parse.evaluate(message);
            }
        });

        MqttPublisherNode mqttPublisherNode = new MqttPublisherNode("publisher",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-pub",
                        "topic", "alert/temp",
                        "qos", 1,
                        "retained", false));


        int modbusPort = RandomGenerator.getDefault().nextInt(500)+500;
        ModbusTcpSimulator modbusTcpSimulator = new ModbusTcpSimulator(modbusPort, 30);

        Flow flow = new Flow("flow");
        flow.addNode(mqttSubscriberNode)
                .addNode(ruleNode)
                .addNode(mqttPublisherNode)
                .connect(mqttSubscriberNode.getId(),"out", ruleNode.getId(), "in")
                .connect(ruleNode.getId(), "match", mqttPublisherNode.getId(), "in");

        modbusTcpSimulator.start();

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);

        flowEngine.startFlow("flow");
    }
}
