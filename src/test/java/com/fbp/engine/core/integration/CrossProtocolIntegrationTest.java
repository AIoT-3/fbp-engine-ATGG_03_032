package com.fbp.engine.core.integration;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.core.rule.RuleExpression;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.external.ModbusReaderNode;
import com.fbp.engine.node.external.ModbusWriterNode;
import com.fbp.engine.node.external.MqttPublisherNode;
import com.fbp.engine.node.external.MqttSubscriberNode;
import com.fbp.engine.node.internal.CollectorNode;
import com.fbp.engine.node.internal.RuleNode;
import com.fbp.engine.protocol.modbus.ModbusTcpSimulator;
import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Slf4j
public class CrossProtocolIntegrationTest {
    @Order(1)
    @Test
    @DisplayName("MQTT -> Rule -> MODBUS")
    void mqttToRuleToModbus() throws IOException, InterruptedException {
        int randomServerPort = RandomUtil.getRandomServerPort();
        String dataPath = "target/moquette_data_" + randomServerPort;

        Properties props = new Properties();
        props.setProperty("port", "" + randomServerPort);
        props.setProperty("host", "0.0.0.0");
        props.setProperty("allow_anonymous", "true");
        props.setProperty("data_path", dataPath);
        props.setProperty("persistence_enabled", "true");

        Server broker = new Server();
        broker.startServer(new MemoryConfig(props));

        MqttSubscriberNode sub = new MqttSubscriberNode("sub1",
                Map.of("brokerUrl", "tcp://localhost:" + randomServerPort,
                        "clientId", "test-sub1",
                        "topic", "test",
                        "qos", 1,
                        "cleanSession", false));

        RuleNode ruleNode = new RuleNode("ruler", new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression parse = RuleExpression.parse("temperature > 36.0");
                return parse.evaluate(message);
            }
        });

        int randomServerPort1 = RandomUtil.getRandomServerPort();
        ModbusTcpSimulator modbusTcpSimulator = new ModbusTcpSimulator(randomServerPort1, 30);
        modbusTcpSimulator.start();

        ModbusWriterNode modbusWriterNode = new ModbusWriterNode("writer", Map.of(
                "host", "localhost",
                "port", randomServerPort1,
                "slaveId", 1,
                "registerAddress", 2,
                "valueField", "temperature"
        ));

        MqttPublisherNode mqttPubStub = new MqttPublisherNode("pub-stub",
                Map.of("brokerUrl", "tcp://localhost:" + randomServerPort,
                        "clientId", "test-pub-stub1",
                        "topic", "test",
                        "qos", 1,
                        "retained", false));

        Flow flow = new Flow("flow");
        flow.addNode(mqttPubStub)
                .addNode(sub)
                .addNode(ruleNode)
                .addNode(modbusWriterNode)
                .connect(sub.getId(),"out", ruleNode.getId(),"in")
                .connect(ruleNode.getId(),"match", modbusWriterNode.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.startFlow("flow");

        mqttPubStub.process("in", new Message(Map.of("temperature", 37)));

        Thread.sleep(60);

        Assertions.assertEquals(37, modbusTcpSimulator.getRegister(2));

        modbusTcpSimulator.stop();
        flowEngine.shutdown();
        broker.stopServer();
    }

    @Order(2)
    @Test
    @DisplayName("MODBUS -> Rule -> MQTT")
    void checkModbusToRuleToMqttPipeLine() throws InterruptedException, IOException {
        int mqttPort = RandomUtil.getRandomServerPort();
        String dataPath = "target/moquette_data_" + mqttPort;

        Properties props = new Properties();
        props.setProperty("port", "" + mqttPort);
        props.setProperty("host", "0.0.0.0");
        props.setProperty("allow_anonymous", "true");
        props.setProperty("data_path", dataPath);
        props.setProperty("persistence_enabled", "true");

        Server broker = new Server();
        broker.startServer(new MemoryConfig(props));

        int modbusPort = RandomUtil.getRandomServerPort();
        ModbusTcpSimulator modbusTcpSimulator = new ModbusTcpSimulator(modbusPort, 30);
        modbusTcpSimulator.start();

        modbusTcpSimulator.setRegister(0, 444);

        ModbusReaderNode modbusReaderNode = new ModbusReaderNode("reader", Map.of(
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
                RuleExpression parse = RuleExpression.parse("temperature > 36");
                return parse.evaluate(message);
            }
        });

        MqttPublisherNode mqttPublisherNode = new MqttPublisherNode("pub",
                Map.of("brokerUrl", "tcp://localhost:" + mqttPort,
                        "clientId", "test-pub1",
                        "topic", "test",
                        "qos", 1,
                        "retained", false));

        MqttSubscriberNode mqttSubStub = new MqttSubscriberNode("sub",
                Map.of("brokerUrl", "tcp://localhost:" + mqttPort,
                        "clientId", "test-sub-stub1",
                        "topic", "test",
                        "qos", 1,
                        "cleanSession", false));

        CollectorNode collectorNode = new CollectorNode("collector");

        Flow flow = new Flow("flow");
        flow.addNode(modbusReaderNode)
                .addNode(ruleNode)
                .addNode(mqttPublisherNode)
                .addNode(mqttSubStub)
                .addNode(collectorNode)
                .connect(modbusReaderNode.getId(), "out", ruleNode.getId(), "in")
                .connect(ruleNode.getId(), "match", mqttPublisherNode.getId(), "in")
                .connect(mqttSubStub.getId(), "out", collectorNode.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.startFlow("flow");

        Thread.sleep(60);

        modbusReaderNode.process("in", new Message(Map.of("trigger", "trigger")));

        Thread.sleep(100);

        Assertions.assertEquals(1, collectorNode.getCollected().size());

        flowEngine.shutdown();
        modbusTcpSimulator.stop();
        broker.stopServer();
    }

    @Order(3)
    @Test
    @DisplayName("복합 플로우 장기 안정성 - 5분 연속 실행")
    void complexFlowLongRunningStability() throws IOException, InterruptedException {
        int mqttPort = RandomUtil.getRandomServerPort();
        int modbusPort = RandomUtil.getRandomServerPort();

        Properties props = new Properties();
        props.setProperty("port", "" + mqttPort);
        props.setProperty("host", "0.0.0.0");
        props.setProperty("allow_anonymous", "true");
        props.setProperty("data_path", "target/moquette_data_" + mqttPort);

        Server broker = new Server();
        broker.startServer(new MemoryConfig(props));

        ModbusTcpSimulator modbusTcpSimulator = new ModbusTcpSimulator(modbusPort, 30);
        modbusTcpSimulator.start();

        // MQTT → Rule → Modbus
        MqttSubscriberNode sub = new MqttSubscriberNode("sub",
                Map.of("brokerUrl", "tcp://localhost:" + mqttPort,
                        "clientId", "stability-sub",
                        "topic", "sensor/temp",
                        "qos", 1,
                        "cleanSession", false));

        RuleNode ruleNode = new RuleNode("ruler", message -> {
            RuleExpression parse = RuleExpression.parse("temperature > 36.0");
            return parse.evaluate(message);
        });

        ModbusWriterNode modbusWriterNode = new ModbusWriterNode("writer", Map.of(
                "host", "localhost",
                "port", modbusPort,
                "slaveId", 1,
                "registerAddress", 2,
                "valueField", "temperature"));

        MqttPublisherNode stub = new MqttPublisherNode("stub",
                Map.of("brokerUrl", "tcp://localhost:" + mqttPort,
                        "clientId", "stability-pub",
                        "topic", "sensor/temp",
                        "qos", 1,
                        "retained", false));

        CollectorNode matchCollector = new CollectorNode("match-collector");
        CollectorNode mismatchCollector = new CollectorNode("mismatch-collector");

        Flow flow = new Flow("flow");
        flow.addNode(stub)
                .addNode(sub)
                .addNode(ruleNode)
                .addNode(modbusWriterNode)
                .addNode(matchCollector)
                .addNode(mismatchCollector)
                .connect(sub.getId(), "out", ruleNode.getId(), "in")
                .connect(ruleNode.getId(), "match", modbusWriterNode.getId(), "in")
                .connect(ruleNode.getId(), "match", matchCollector.getId(), "in")
                .connect(ruleNode.getId(), "mismatch", mismatchCollector.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.startFlow("flow");

        Thread.sleep(300);

        long testDurationMs = 5 * 60 * 1000L;
        long intervalMs = 100;
        long startTime = System.currentTimeMillis();
        AtomicInteger errorCount = new AtomicInteger();
        AtomicInteger sentCount = new AtomicInteger();

        Random random = new Random();
        while (System.currentTimeMillis() - startTime < testDurationMs) {
            double temp = 30.0 + random.nextDouble() * 15.0;
            try {
                stub.process("in", new Message(Map.of("temperature", temp)));
                sentCount.incrementAndGet();
            } catch (Exception e) {
                errorCount.incrementAndGet();
                log.warn("발행 실패: {}", e.getMessage());
            }
            Thread.sleep(intervalMs);
        }

        Thread.sleep(500);

        long elapsed = System.currentTimeMillis() - startTime;
        int sent = sentCount.get();
        int matched = matchCollector.getCollected().size();
        int mismatched = mismatchCollector.getCollected().size();
        int received = matched + mismatched;

        System.out.printf("""
            ┌──────────────────────────────────────┐
            │ 복합 플로우 장기 안정성 테스트 결과    │
            ├──────────────────────────────────────┤
            │ 실행 시간:   %5d 초                  │
            │ 발행 메시지: %5d 건                  │
            │ 수신 메시지: %5d 건                  │
            │ match:       %5d 건                  │
            │ mismatch:    %5d 건                  │
            │ 발행 에러:   %5d 건                  │
            │ 수신률:      %5.1f %%                 │
            └──────────────────────────────────────┘
            """,
                elapsed / 1000, sent, received,
                matched, mismatched, errorCount.get(),
                sent > 0 ? received * 100.0 / sent : 0);

        Assertions.assertEquals(0, errorCount.get(), "발행 에러 발생");
        Assertions.assertTrue(received >= sent * 0.95,
                String.format("수신률 95%% 미달: sent=%d, received=%d", sent, received));

        flowEngine.shutdown();
        modbusTcpSimulator.stop();
        broker.stopServer();
    }

}
