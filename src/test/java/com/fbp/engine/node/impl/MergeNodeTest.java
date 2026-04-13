package com.fbp.engine.node.impl;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MergeNodeTest {
    @Spy
    MergeNode target = new MergeNode("target");
    @Mock
    Connection connModule;


    @BeforeEach
    void setUp(){
        target.getOutputPort("out").connect(connModule);
    }

    @Order(1)
    @Test
    @DisplayName("양쪽 입력 수신")
    void receiveBothInputs(){
        InputPort inputPort1 = target.getInputPort("in-1");
        InputPort inputPort2 = target.getInputPort("in-2");
        Assertions.assertAll(
                () -> Assertions.assertNotNull(inputPort1),
                () -> Assertions.assertNotNull(inputPort2)
        );

        Message message = new Message(Map.of("test","value"));

        Assertions.assertDoesNotThrow(() -> {
            inputPort1.receive(message);
            inputPort2.receive(message);
        });
    }

    @Order(2)
    @Test
    @DisplayName("합쳐진 메시지 출력")
    void sendMergedMessage(){
        Connection connection = new Connection("cooonnnn");
        target.getOutputPort("out").connect(connection);

        InputPort inputPort1 = target.getInputPort("in-1");
        InputPort inputPort2 = target.getInputPort("in-2");
        Assertions.assertAll(
                () -> Assertions.assertNotNull(inputPort1),
                () -> Assertions.assertNotNull(inputPort2)
        );

        Message message1 = new Message(Map.of("test1","value1"));
        Message message2 = new Message(Map.of("test2", "value2"));

        Assertions.assertDoesNotThrow(() -> {
            inputPort1.receive(message1);
            inputPort2.receive(message2);
        });

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Message message = connection.poll();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        String msgString = message.toString();

        Assertions.assertTrue(msgString.contains("test1=value1") && msgString.contains("test2=value2"));
    }

    @Order(3)
    @DisplayName("한쪽만 도착 시 대기")
    @Test
    void waitWhenOnlyOneSideArrive(){
        InputPort inputPort1 = target.getInputPort("in-1");
        Assertions.assertNotNull(inputPort1);

        Message message1 = new Message(Map.of("test1","value1"));

        Assertions.assertDoesNotThrow(() -> {
            inputPort1.receive(message1);
        });

        verify(connModule, never()).deliver(any());
    }

    @Order(4)
    @Test
    @DisplayName("포트 구성 확인")
    void checkPortConfiguration(){
        Assertions.assertAll(
                () -> Assertions.assertNotNull(target.getInputPort("in-1")),
                () -> Assertions.assertNotNull(target.getInputPort("in-2")),
                () -> Assertions.assertNotNull(target.getOutputPort("out"))
        );
    }
}
