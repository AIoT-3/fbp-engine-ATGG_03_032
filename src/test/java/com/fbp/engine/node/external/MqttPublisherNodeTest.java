package com.fbp.engine.node.external;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;

import java.util.Map;

public class MqttPublisherNodeTest {
    MqttPublisherNode publisherNode;

    @BeforeEach
    void setUp(){
        publisherNode = new MqttPublisherNode("publisher",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-pub",
                        "topic", "sensor/temp",
                        "qos", 1,
                        "retained", false));
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
    void ifProcessThenBrokerReceiving(){
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode("subscriber",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-sub",
                        "topic", "sensor/temp",
                        "qos", 1));
        Connection connection = new Connection("conn-1");
        subscriberNode.getOutputPort("out").connect(connection);

        subscriberNode.initialize();
        publisherNode.initialize();

        Message message = new Message(Map.of("temperature", 35.5));
        publisherNode.process(null,message);

        Assertions.assertEquals(35.5, connection.poll().get("temperature"));

        subscriberNode.shutdown();
        publisherNode.shutdown();
    }

    @Tag("integration")
    @Order(6)
    @Test
    @DisplayName("동적 토픽")
    void ifMessageContainTopicKeyThenPublishToThatTopic(){
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode("subscriber",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-sub",
                        "topic", "sensor/topic",
                        "qos", 1));
        Connection connection = new Connection("conn-1");
        subscriberNode.getOutputPort("out").connect(connection);

        subscriberNode.initialize();
        publisherNode.initialize();

        Message message = new Message(
                Map.of("temperature", 35.5,
                        "targetTopic", "sensor/topic"
                ));
        publisherNode.process(null,message);

        Assertions.assertEquals(35.5, connection.poll().get("temperature"));

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
