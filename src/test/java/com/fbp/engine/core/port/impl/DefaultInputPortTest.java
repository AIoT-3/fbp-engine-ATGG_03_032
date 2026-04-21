package com.fbp.engine.core.port.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.core.node.Node;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class DefaultInputPortTest {
    @Mock
    Node mockOwner;

    DefaultInputPort defaultInputPort;

    @BeforeEach
    void setUp(){
        defaultInputPort = new DefaultInputPort("in", mockOwner);
    }

    @Order(1)
    @Test
    @DisplayName("receive 시 owner 호출")
    void shouldCallOwnerProcessWhenReceive() {
        Message testMessage = new Message(Map.of("test", "value"));

        defaultInputPort.receive(testMessage);

        Mockito.verify(mockOwner, Mockito.times(1)).process("in",testMessage);
    }

    @Order(2)
    @Test
    @DisplayName("포트 이름 확인")
    void shouldReturnPortName() {
        Assertions.assertEquals("in", defaultInputPort.getName());
    }

}
