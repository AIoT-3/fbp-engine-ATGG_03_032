package com.fbp.engine.core.node;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.internal.TemperatureSensorNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TemperatureSensorNodeTest {
    @Spy
    TemperatureSensorNode node = new TemperatureSensorNode("test-sensor", 15.0, 45.0);

    Message triggerMessage;

    @BeforeEach
    void setUp() {
        triggerMessage = new Message(Map.of("test", "value"));
    }

    @Order(1)
    @Test
    @DisplayName("온도 범위 확인: 생성된 온도가 min~max 범위 이내")
    void testTemperatureRange() {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

        node.onProcess("trigger", triggerMessage);

        verify(node, times(1)).send(eq("out"), captor.capture());

        Message sentMessage = captor.getValue();
        Double temp = (Double) sentMessage.get("temperature");

        Assertions.assertNotNull(temp);
        Assertions.assertTrue(temp >= 15.0 && temp <= 45.0,
                "생성된 온도(" + temp + ")가 범위를 벗어났습니다.");
    }

    @Order(2)
    @Test
    @DisplayName("필수 키 포함: 출력 메시지에 필수 정보 존재")
    void testRequiredKeys() {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

        node.onProcess("trigger", triggerMessage);

        verify(node).send(eq("out"), captor.capture());
        Message sentMessage = captor.getValue();

        Assertions.assertAll(
                () -> Assertions.assertNotNull(sentMessage.get("sensorId"), "sensorId 누락"),
                () -> Assertions.assertNotNull(sentMessage.get("temperature"), "temperature 누락"),
                () -> Assertions.assertNotNull(sentMessage.get("unit"), "unit 누락"),
                () -> Assertions.assertNotNull(sentMessage.get("timestamp"), "timestamp 누락")
        );
    }

    @Order(3)
    @Test
    @DisplayName("sensorId 일치: 메시지의 sensorId가 노드 ID와 일치")
    void testSensorIdMatch() {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

        node.onProcess("trigger", triggerMessage);

        verify(node).send(eq("out"), captor.capture());
        String sensorId = (String) captor.getValue().get("sensorId");

        Assertions.assertEquals("test-sensor", sensorId);
    }

    @Order(4)
    @Test
    @DisplayName("트리거마다 생성: 트리거 메시지 3번 전송 시 3개의 결과 생성")
    void testMultipleTriggers() {
        node.onProcess("trigger", triggerMessage);
        node.onProcess("trigger", triggerMessage);
        node.onProcess("trigger", triggerMessage);

        verify(node, times(3)).send(eq("out"), any(Message.class));
    }
}