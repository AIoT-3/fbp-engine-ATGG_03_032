package com.fbp.engine.node.impl;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;


@ExtendWith(MockitoExtension.class)
public class AlertNodeTest {
    @Spy
    AlertNode target = new AlertNode("target");

    @Test
    @DisplayName("정상 처리 - onProcess 직접 검증")
    void successfullyProcess(){
        Message m = new Message(Map.of("sensorId", "sensor-123", "temperature", 36.5));

        target.process("in", m);

        Mockito.verify(target, Mockito.times(1)).onProcess(Mockito.any(), Mockito.any());
    }

    @Test
    @DisplayName("키 누락 시 처리 - 예외 발생 여부 확인")
    void InCaseOfMissingKey(){
        Message message = new Message(Map.of("undefinedKey", "undefinedValue"));

        Assertions.assertDoesNotThrow(() -> target.process("in", message));
    }
}