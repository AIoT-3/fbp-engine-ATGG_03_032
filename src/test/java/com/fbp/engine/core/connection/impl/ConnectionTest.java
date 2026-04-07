package com.fbp.engine.core.connection.impl;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ConnectionTest {
    @Mock
    InputPort inputPort;

    Connection connection;

    @BeforeEach
    void setUp(){
        connection = new Connection("test");
        connection.setTarget(inputPort);
    }

    @Order(1)
    @Test
    @DisplayName("deliver 후 target 수신")
    void shouldDeliverMessageToTarget(){
        Message testMessage = new Message(Map.of("test", "value"));

        connection.deliver(testMessage);

        verify(inputPort, times(1))
                .receive(testMessage);
    }

    @Order(2)
    @Test
    @DisplayName("target 미설정 시 동작")
    void ifTargetNullNotThrowException(){
        Connection ttestt = new Connection("ttestt");

        Assertions.assertDoesNotThrow(
                () -> ttestt.deliver(new Message(Map.of("test", "value")))
        );
    }

    @Order(3)
    @Test
    @DisplayName("버퍼 크기 확인")
    void shouldStoreMessageInBufferWhenTargetIsNull(){
        Connection ttestt = new Connection("ttestt");
        Message testMessage = new Message(Map.of("test", "value"));

        ttestt.deliver(testMessage);

        Assertions.assertEquals(1, ttestt.getBufferSize());
    }

    @Order(4)
    @Test
    @DisplayName("다수 메시지 순서 보장")
    void shouldPreserveMessageOrder(){
        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        int count = 100;

        for(int i=0; i < count; i++) {
            Message testMessage = new Message(Map.of("test" + i, "value" + i));
            connection.deliver(testMessage);
        }

        verify(inputPort, times(count)).receive(messageCaptor.capture());

        List<Message> capturedMessages = messageCaptor.getAllValues();

        for (int i = 0; i < count; i++) {
            Assertions.assertEquals("value"+i, capturedMessages.get(i).get("test"+i));
        }
    }
}
