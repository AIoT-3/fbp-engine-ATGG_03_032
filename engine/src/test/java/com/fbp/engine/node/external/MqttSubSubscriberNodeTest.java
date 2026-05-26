package com.fbp.engine.node.external;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.node.internal.CollectorNode;
import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MqttSubSubscriberNodeTest {
    MqttSubscriberNode mqttSubscriberNode;
    private Server broker;

    @BeforeEach
    void setUp() throws IOException {
        int brokerPort = RandomUtil.getRandomServerPort();
        String dataPath = "target/moquette_data_" + brokerPort;

        Properties brokerProps = new Properties();
        brokerProps.setProperty("port", String.valueOf(brokerPort));
        brokerProps.setProperty("host", "0.0.0.0");
        brokerProps.setProperty("allow_anonymous", "true");
        brokerProps.setProperty("data_path", dataPath);
        brokerProps.setProperty("persistence_enabled", "false");

        broker = new Server();
        broker.startServer(new MemoryConfig(brokerProps));

        String uniqueId = "sub-" + System.currentTimeMillis();
        mqttSubscriberNode = new MqttSubscriberNode("subscriber",
                Map.of("brokerUrl", "tcp://localhost:" + brokerPort,
                        "clientId", uniqueId,
                        "topic", "sensor/temp",
                        "qos", 1));
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
        Assertions.assertNotNull(mqttSubscriberNode.getOutputPort("out"));
    }

    @Order(2)
    @Test
    @DisplayName("초기 상태")
    void checkInitState(){
        Assertions.assertFalse(mqttSubscriberNode.isConnected());
    }

    @Order(3)
    @Test
    @DisplayName("config 조회")
    void getConfig(){
        String expectedUrl = (String) mqttSubscriberNode.getConfig("brokerUrl");
        assertEquals(expectedUrl, mqttSubscriberNode.getConfig("brokerUrl"));
    }

    @Order(4)
    @Test
    @DisplayName("JSON -> Message 변환")
    void jsonToMessageMethodTest(){
        String json = "{\"temperature\": 25.5}";
        byte[] payloadBytes = json.getBytes();

        Map<String, Object> result = mqttSubscriberNode.processPayload("test/topic", payloadBytes);

        Assertions.assertEquals(25.5, result.get("temperature"));
        Assertions.assertEquals("test/topic", result.get("sourceTopic"));
        Assertions.assertTrue(result.containsKey("mqttTimestamp"));
    }

    @Order(5)
    @Test
    @DisplayName("JSON 파싱 실패 처리")
    void jsonParsingFailedProcessing(){
        String failedJson = "{\"temperature\": 25.5";
        byte[] payloadBytes = failedJson.getBytes();

        Map<String, Object> result = mqttSubscriberNode.processPayload("test/topic", payloadBytes);
        Assertions.assertNotNull(result.get("rawPayload"));
    }

    @Tag("integration")
    @Order(6)
    @Test
    @DisplayName("Broker 연결 성공")
    void brokerConnectSuccess(){
        mqttSubscriberNode.initialize();

        Assertions.assertTrue(mqttSubscriberNode.isConnected());
    }

    @Tag("integration")
    @Order(7)
    @Test
    @DisplayName("메시지 수신")
    void messageReceiving() throws Exception {
        CollectorNode collectorNode = new CollectorNode("collector");

        Connection connection = new Connection("conn-1");
        mqttSubscriberNode.getOutputPort("out").connect(connection);
        connection.setTarget(collectorNode.getInputPort("in"));

        mqttSubscriberNode.initialize();

        String brokerUrl = (String) mqttSubscriberNode.getConfig("brokerUrl");
        String topic = (String) mqttSubscriberNode.getConfig("topic");
        String content = "{\"temperature\": 25.5}";

        MqttClient testClient = new MqttClient(brokerUrl, "test-pub-" + System.currentTimeMillis());
        testClient.connect();
        Thread.sleep(500);
        testClient.publish(topic, new org.eclipse.paho.mqttv5.common.MqttMessage(content.getBytes()));
        Thread.sleep(500);

        testClient.disconnect();
        testClient.close();

        connection.poll();
        Thread.sleep(500);

        Assertions.assertFalse(collectorNode.getCollected().isEmpty());
    }

    @Tag("integration")
    @Order(8)
    @Test
    @DisplayName("토픽 정보 포함")
    void checkContainTopic() throws MqttException, InterruptedException {
        CollectorNode collectorNode = new CollectorNode("collector");

        Connection connection = new Connection("conn-1");
        mqttSubscriberNode.getOutputPort("out").connect(connection);
        connection.setTarget(collectorNode.getInputPort("in"));

        mqttSubscriberNode.initialize();

        String brokerUrl = (String) mqttSubscriberNode.getConfig("brokerUrl");
        String topic = (String) mqttSubscriberNode.getConfig("topic");
        String content = "{\"temperature\": 25.5}";

        MqttClient testClient = new MqttClient(brokerUrl, "test-pub-" + System.currentTimeMillis());
        testClient.connect();
        Thread.sleep(500);
        testClient.publish(topic, new org.eclipse.paho.mqttv5.common.MqttMessage(content.getBytes()));
        Thread.sleep(500);

        testClient.disconnect();
        testClient.close();

        connection.poll();
        Thread.sleep(500);

        Assertions.assertTrue(collectorNode.getCollected().stream().findFirst().get().hasKey("sourceTopic"));
    }

    @Tag("integration")
    @Order(9)
    @Test
    @DisplayName("shutdown 후 연결 해제")
    void ifShutdownThenDisconnect(){
        Assertions.assertFalse(mqttSubscriberNode.isConnected());

        mqttSubscriberNode.initialize();
        Assertions.assertTrue(mqttSubscriberNode.isConnected());

        mqttSubscriberNode.shutdown();
        Assertions.assertFalse(mqttSubscriberNode.isConnected());
    }
}