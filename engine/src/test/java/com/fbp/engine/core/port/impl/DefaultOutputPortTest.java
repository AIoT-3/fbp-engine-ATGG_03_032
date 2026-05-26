package com.fbp.engine.core.port.impl;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class DefaultOutputPortTest {
    @Mock
    Connection connection;
    @Mock
    Connection connection1;
    @Mock
    Connection connection2;

    DefaultOutputPort defaultOutputPort;

    @BeforeEach
    void setUp(){
        defaultOutputPort = new DefaultOutputPort("out");
    }

    @Order(1)
    @Test
    @DisplayName("단일 Connection 전달")
    void sendSingleConnection(){
        defaultOutputPort.connect(connection);

        defaultOutputPort.send(new Message(
                Map.of("test", "value")
        ));

        Mockito.verify(connection, Mockito.times(1)).deliver(any());
    }

    @Order(2)
    @Test
    @DisplayName("다중 Connection 전달 (1:N)")
    void sendMultiConnections(){
        defaultOutputPort.connect(connection);
        defaultOutputPort.connect(connection1);
        defaultOutputPort.connect(connection2);

        defaultOutputPort.send(new Message(
                Map.of("test", "value")
        ));

        Mockito.verify(connection, Mockito.times(1)).deliver(any());
        Mockito.verify(connection1, Mockito.times(1)).deliver(any());
        Mockito.verify(connection2, Mockito.times(1)).deliver(any());
    }

    @Order(3)
    @Test
    @DisplayName("Connection 미연결 시")
    void ifNotConnectedDoseNotThrowException(){
        Assertions.assertDoesNotThrow(
                () -> defaultOutputPort.send(new Message(
                        Map.of("test", "value")
                ))
        );
    }
}
