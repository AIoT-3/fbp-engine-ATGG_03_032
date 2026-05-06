package com.fbp.engine.core.flow;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.external.MqttPublisherNode;
import com.fbp.engine.node.external.MqttSubscriberNode;
import com.fbp.engine.node.internal.CollectorNode;

import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MqttIntegrationTest {


    @Order(1)
    @Test
    @DisplayName("Subscriber -> Publisher 파이프라인")
    void checkSubscriberToPublisherPipeLine() throws InterruptedException {
        MqttSubscriberNode mqttSubscriberNode = new MqttSubscriberNode("mqtt-sub1",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-sub1",
                        "topic", "temperature",
                        "qos", 1));

        MqttPublisherNode mqttPublisherNode = new MqttPublisherNode("mqtt-pub1",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-pub1",
                        "topic", "myTopic/temperature",
                        "qos", 1,
                        "retained", false));

        MqttPublisherNode pubStup = new MqttPublisherNode("pubStub1",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "pup-test-stup1",
                        "topic", "temperature",
                        "qos", 1,
                        "retained", false));

        MqttSubscriberNode subStup= new MqttSubscriberNode("customTopic-sub1",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "sub-test-stup1",
                        "topic", "myTopic/temperature",
                        "qos", 1));


        CollectorNode collectorStub = new CollectorNode("stub");

        Flow flow = new Flow("flow");
        flow.addNode(pubStup)
                .addNode(mqttSubscriberNode)
                .addNode(mqttPublisherNode)
                .addNode(subStup)
                .addNode(collectorStub)
                .connect(mqttSubscriberNode.getId(), "out", mqttPublisherNode.getId(), "in")
                .connect(subStup.getId(),"out", collectorStub.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);

        flowEngine.startFlow("flow");

        Thread.sleep(50);

        pubStup.process("in", new Message(Map.of("value", 36.5)));

        Thread.sleep(50);

        Assertions.assertTrue(collectorStub.getCollected().get(0).toString().contains("myTopic/temperature"));

        flowEngine.shutdown();
    }

    @Order(2)
    @Test
    @DisplayName("다중 토픽 구독")
    void subscribeMultiTopic() throws InterruptedException {
        MqttSubscriberNode mqttSubscriberNode = new MqttSubscriberNode("mqtt-sub2",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-sub2",
                        "topic", "sensor/+",
                        "qos", 1));

        CollectorNode collectorStub = new CollectorNode("stub");

        MqttPublisherNode pubStup1 = new MqttPublisherNode("pubStub2",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "pup-test-stup2",
                        "topic", "sensor/temperature",
                        "qos", 1,
                        "retained", false));

        MqttPublisherNode pubStup2 = new MqttPublisherNode("pubStub22",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "pup-test-stup22",
                        "topic", "sensor/pressure",
                        "qos", 1,
                        "retained", false));

        Flow flow = new Flow("flow");
        flow.addNode(mqttSubscriberNode)
                .addNode(collectorStub)
                .addNode(pubStup1)
                .addNode(pubStup2)
                .connect(mqttSubscriberNode.getId(),"out", collectorStub.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);

        flowEngine.startFlow("flow");

        pubStup1.process("in", new Message(Map.of("value", 36.5)));
        pubStup2.process("in", new Message(Map.of("value", 1023)));

        Thread.sleep(50);

        Assertions.assertEquals(2, collectorStub.getCollected().size());

        flowEngine.shutdown();
    }

    @Order(3)
    @Test
    @DisplayName("Qos 1 전달 보장")
    void qos1GuaranteedDelivery() throws Exception {
        int port = RandomUtil.getRandomServerPort();
        String dataPath = "target/moquette_data_" + port;

        Properties props = new Properties();
        props.setProperty("port", ""+port);
        props.setProperty("host", "0.0.0.0");
        props.setProperty("allow_anonymous", "true");
        props.setProperty("data_path", dataPath);
        props.setProperty("persistence_enabled", "true");

        Server broker = new Server();
        broker.startServer(new MemoryConfig(props));

        MqttSubscriberNode sub = new MqttSubscriberNode("sub3",
                Map.of("brokerUrl", "tcp://localhost:" + port,
                        "clientId", "test-sub3",
                        "topic", "qos-test",
                        "qos", 1,
                        "cleanSession", false));

        MqttPublisherNode pub = new MqttPublisherNode("pub3",
                Map.of("brokerUrl", "tcp://localhost:" + port,
                        "clientId", "test-pub3",
                        "topic", "qos-test",
                        "qos", 1,
                        "retained", false));

        CollectorNode collector = new CollectorNode("collector");

        Flow flow = new Flow("flow");
        flow.addNode(pub).addNode(sub).addNode(collector)
                .connect(sub.getId(), "out", collector.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.startFlow("flow");

        Thread.sleep(300);

        sub.disconnect();
        sub.shutdown();
        Thread.sleep(100);

        pub.process("in", new Message(Map.of("value", 99.9)));
        Thread.sleep(100);

        Assertions.assertEquals(0, collector.getCollected().size());

        sub.reconnect();
        Thread.sleep(6000);

        long deadline = System.currentTimeMillis() + 10000;
        while ((!pub.isConnected() || !sub.isConnected()) && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }

        Assertions.assertEquals(1, collector.getCollected().size());

        flowEngine.shutdown();
        broker.stopServer();
    }

    @Order(4)
    @Test
    @DisplayName("재연결 테스트")
    void testReconnect() throws IOException, InterruptedException {
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

        MqttSubscriberNode sub = new MqttSubscriberNode("sub4",
                Map.of("brokerUrl", "tcp://localhost:" + randomServerPort,
                        "clientId", "test-sub4",
                        "topic", "qos-test",
                        "qos", 1,
                        "cleanSession", false));

        MqttPublisherNode pub = new MqttPublisherNode("pub4",
                Map.of("brokerUrl", "tcp://localhost:" + randomServerPort,
                        "clientId", "test-pub4",
                        "topic", "qos-test",
                        "qos", 1,
                        "retained", false));

        CollectorNode collector = new CollectorNode("collector");

        Flow flow = new Flow("flow");
        flow.addNode(sub)
                .addNode(pub)
                .addNode(collector)
                .connect(sub.getId(),"out", collector.getId(),"in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.startFlow("flow");

        broker.stopServer();
        Thread.sleep(500);

        broker.startServer(props);
        long deadline = System.currentTimeMillis() + 10000;
        while ((!pub.isConnected() || !sub.isConnected()) && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }

        pub.process("in", new Message(Map.of("value", 36.5)));

        Thread.sleep(500);
        Assertions.assertEquals(1, collector.getCollected().size());

        broker.stopServer();
        flowEngine.shutdown();
    }
}
