package com.fbp.engine.integration;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.engine.FlowEngine;
import com.fbp.engine.core.rule.RuleExpression;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.external.ModbusWriterNode;
import com.fbp.engine.node.external.MqttPublisherNode;
import com.fbp.engine.node.external.MqttSubscriberNode;
import com.fbp.engine.node.internal.CollectorNode;
import com.fbp.engine.node.internal.RuleNode;
import com.fbp.engine.node.internal.TemperatureSensorNode;
import com.fbp.engine.node.internal.TimerNode;
import com.fbp.engine.protocol.modbus.ModbusTcpSimulator;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;


@Tag("integration")
public class MqttModbusIntegrationTest {
    MqttPublisherNode mqttPublisherNode;
    MqttSubscriberNode mqttSubscriberNode;

    RuleNode ruleNode;

    int modbusPort = -1;
    ModbusTcpSimulator modbusTcpSimulator;
    ModbusWriterNode modbusWriterNode;

    CollectorNode collectorNode1;
    CollectorNode collectorNode2;

    @BeforeEach
    void setUp(){
        mqttPublisherNode = new MqttPublisherNode("publisher",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-pub",
                        "topic", "temperature",
                        "qos", 1,
                        "retained", false));

        mqttSubscriberNode = new MqttSubscriberNode("subscriber",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-sub",
                        "topic", "temperature",
                        "qos", 1));

        ruleNode = new RuleNode("ruler", new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression parse = RuleExpression.parse("temperature >= 35.0");

                return parse.evaluate(message);
            }
        });

        modbusPort = RandomGenerator.getDefault().nextInt(500)+500;
        modbusTcpSimulator = new ModbusTcpSimulator(modbusPort, 30);
        modbusWriterNode = new ModbusWriterNode("writer", Map.of(
                "host", "localhost",
                "port", modbusPort,
                "slaveId", 1,
                "registerAddress", 2,
                "valueField", "temperature"));

        collectorNode1 = new CollectorNode("collector-1");
        collectorNode2 = new CollectorNode("collector-2");
    }

    @Order(1)
    @Test
    @DisplayName("MQTT 수신 -> Rule 분기")
    void mqttSubAndRuleBranch() throws InterruptedException {
        Flow flow = new Flow("flow");

        flow.addNode(mqttPublisherNode)
                .addNode(mqttSubscriberNode)
                .addNode(ruleNode)
                .addNode(collectorNode1)
                .addNode(collectorNode2)
                .connect(mqttSubscriberNode.getId(), "out", ruleNode.getId(), "in")
                .connect(ruleNode.getId(), "match", collectorNode1.getId(), "in")
                .connect(ruleNode.getId(), "mismatch", collectorNode2.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);

        modbusTcpSimulator.start();
        Thread.sleep(50);

        flowEngine.startFlow("flow");

        mqttPublisherNode.process("in", new Message(Map.of("temperature", 35.0)));

        Thread.sleep(50);

        Assertions.assertEquals(1, collectorNode1.getCollected().size());

        flowEngine.shutdown();
        modbusTcpSimulator.stop();
    }

    @Order(2)
    @Test
    @DisplayName("Rule match -> MODBUS 쓰기")
    void rulerMatchToModbusWriter() throws InterruptedException {
        Flow flow = new Flow("flow");

        flow.addNode(ruleNode)
                .addNode(modbusWriterNode)
                .connect(ruleNode.getId(),"match", modbusWriterNode.getId(),"in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);

        modbusTcpSimulator.start();
        Thread.sleep(20);
        flowEngine.startFlow("flow");

        ruleNode.process("in", new Message(Map.of("temperature", 36.0)));
        Thread.sleep(20);

        Assertions.assertEquals(36.0, modbusTcpSimulator.getRegister(2));

        flowEngine.shutdown();
        modbusTcpSimulator.stop();
    }

    @Order(3)
    @Test
    @DisplayName("Rule match -> MQTT 알림")
    void rulerMatchToMqttAlert() throws InterruptedException {
        Flow flow = new Flow("flow");

        flow.addNode(ruleNode)
                .addNode(mqttPublisherNode)
                .addNode(mqttSubscriberNode)
                .addNode(collectorNode1)
                .connect(ruleNode.getId(), "match", mqttPublisherNode.getId(), "in")
                .connect(mqttSubscriberNode.getId(), "out", collectorNode1.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);

        modbusTcpSimulator.start();
        Thread.sleep(20);

        flowEngine.startFlow("flow");

        ruleNode.process("in", new Message(Map.of("temperature", 36.0)));
        Thread.sleep(50);

        Assertions.assertEquals(1, collectorNode1.getCollected().size());

        flowEngine.shutdown();
        modbusTcpSimulator.stop();
    }

    @Order(4)
    @Test
    @DisplayName("End-to-End 흐름")
    void entirePipelineOperatesWithoutInterruption() throws InterruptedException {
        Flow flow = new Flow("flow");

        TemperatureSensorNode temperatureSensorNode = new TemperatureSensorNode("sensor-temperature", 20.0, 45.0);
        TimerNode timerNode = new TimerNode("trigger", 500);
        MqttPublisherNode thisFlowPub = new MqttPublisherNode("publisher",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-pub-alerter",
                        "topic", "temperature-alert",
                        "qos", 1,
                        "retained", false));

        flow.addNode(mqttSubscriberNode)
                .addNode(ruleNode)
                .addNode(modbusWriterNode)
                .addNode(thisFlowPub)
                .addNode(collectorNode1)
                .addNode(collectorNode2)
                .connect(mqttSubscriberNode.getId(), "out", ruleNode.getId(), "in")
                .connect(ruleNode.getId(),"match", modbusWriterNode.getId(), "in")
                .connect(ruleNode.getId(), "match", thisFlowPub.getId(), "in")
                .connect(ruleNode.getId(), "match", collectorNode1.getId(), "in")
                .connect(ruleNode.getId(), "mismatch", collectorNode2.getId(), "in");

        Flow flow1 = new Flow("flow1");
        flow1.addNode(timerNode)
                .addNode(temperatureSensorNode)
                .addNode(mqttPublisherNode)
                .connect(timerNode.getId(), "out", temperatureSensorNode.getId(), "trigger")
                .connect(temperatureSensorNode.getId(), "out", mqttPublisherNode.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.register(flow1);

        modbusTcpSimulator.start();
        Thread.sleep(50);
        flowEngine.startFlow("flow");
        flowEngine.startFlow("flow1");

        Assertions.assertDoesNotThrow(()->Thread.sleep(10000));

        modbusTcpSimulator.stop();

        flowEngine.stopFlow("flow1");
        flowEngine.stopFlow("flow");
        flowEngine.shutdown();
    }
}
