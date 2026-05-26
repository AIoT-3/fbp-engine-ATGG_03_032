package com.fbp.engine.node.external;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MqttPublisherNodeTest {
    MqttPublisherNode publisherNode;
    private Server broker;
    private int brokerPort;

    @BeforeEach
    void setUp() throws IOException {
        brokerPort = RandomUtil.getRandomServerPort();
        String dataPath = "target/moquette_data_pub_" + brokerPort;

        Properties brokerProps = new Properties();
        brokerProps.setProperty("port", String.valueOf(brokerPort));
        brokerProps.setProperty("host", "0.0.0.0");
        brokerProps.setProperty("allow_anonymous", "true");
        brokerProps.setProperty("data_path", dataPath);
        brokerProps.setProperty("persistence_enabled", "false");

        broker = new Server();
        broker.startServer(new MemoryConfig(brokerProps));

        publisherNode = new MqttPublisherNode("publisher",
                Map.of("brokerUrl", "tcp://localhost:" + brokerPort,
                        "clientId", "test-pub",
                        "topic", "sensor/temp",
                        "qos", 1,
                        "retained", false));
    }

    @AfterEach
    void tearDown() {
        if (broker != null) {
            broker.stopServer();
        }
    }

    @Order(1)
    @Test
    @DisplayName("포트 구성")
    void checkPortConfiguration(){
        Assertions.assertNotNull(publisherNode.getInputPort("in"));
    }

    @Order(2)
    @Test
    @DisplayName("초기 상태")
    void checkInitConnectionState(){
        Assertions.assertFalse(publisherNode.isConnected());
    }

    @Order(3)
    @Test
    @DisplayName("config 기본 토픽 조회")
    void checkInitConfigTopic(){
        Assertions.assertEquals("sensor/temp", publisherNode.getConfig("topic"));
    }

    @Tag("integration")
    @Order(4)
    @Test
    @DisplayName("Broker 연결 성공")
    void ifInitializeThenConnected(){
        publisherNode.initialize();

        Assertions.assertTrue(publisherNode.isConnected());
    }

    @Tag("integration")
    @Order(5)
    @Test
    @DisplayName("메시지 발행")
    void ifProcessThenBrokerReceiving() throws InterruptedException {
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode("subscriber",
                Map.of("brokerUrl", "tcp://localhost:" + brokerPort,
                        "clientId", "test-sub-" + System.currentTimeMillis(),
                        "topic", "sensor/temp",
                        "qos", 1));

        Connection connection = new Connection("conn-1");
        subscriberNode.getOutputPort("out").connect(connection);

        subscriberNode.initialize();
        publisherNode.initialize();

        Thread.sleep(500);

        Message message = new Message(Map.of("temperature", 35.5));
        publisherNode.process("in", message);

        Thread.sleep(500);

        Message received = connection.poll();
        Assertions.assertNotNull(received, "메시지를 수신하지 못했습니다.");
        Assertions.assertEquals(35.5, received.get("temperature"));

        subscriberNode.shutdown();
        publisherNode.shutdown();
    }

    @Tag("integration")
    @Order(6)
    @Test
    @DisplayName("동적 토픽")
    void ifMessageContainTopicKeyThenPublishToThatTopic() throws InterruptedException {
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode("subscriber",
                Map.of("brokerUrl", "tcp://localhost:" + brokerPort,
                        "clientId", "test-sub-" + System.currentTimeMillis(),
                        "topic", "sensor/topic",
                        "qos", 1));

        Connection connection = new Connection("conn-1");
        subscriberNode.getOutputPort("out").connect(connection);

        subscriberNode.initialize();
        publisherNode.initialize();

        Thread.sleep(500);

        Message message = new Message(
                Map.of("temperature", 35.5,
                        "targetTopic", "sensor/topic"
                ));
        publisherNode.process("in", message);

        Thread.sleep(500);

        Message received = connection.poll();
        Assertions.assertNotNull(received, "동적 토픽 메시지를 수신하지 못했습니다.");
        Assertions.assertEquals(35.5, received.get("temperature"));

        subscriberNode.shutdown();
        publisherNode.shutdown();
    }

    @Tag("integration")
    @Order(7)
    @Test
    @DisplayName("shutdown 후 연결 해제")
    void ifShutDownThenDisconnected(){
        Assertions.assertFalse(publisherNode.isConnected());

        publisherNode.initialize();
        Assertions.assertTrue(publisherNode.isConnected());

        publisherNode.shutdown();
        Assertions.assertFalse(publisherNode.isConnected());
    }
}