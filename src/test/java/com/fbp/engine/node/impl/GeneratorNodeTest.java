package com.fbp.engine.node.impl;

import com.fbp.engine.core.port.OutputPort;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GeneratorNodeTest {
    private GeneratorNode generatorNode;

    @Mock
    private OutputPort mockOutputPort;

    @BeforeEach
    void setUp() {
        generatorNode = new GeneratorNode("gen-1");
        generatorNode.setOutputPort(mockOutputPort);
    }

    @Order(1)
    @Test
    @DisplayName("generate 메시지 생성")
    void generateShouldSendMessageToOutputPort() {
        generatorNode.generate("key", "value");

        verify(mockOutputPort, times(1)).send(Mockito.any(Message.class));
    }

    @Order(2)
    @Test
    @DisplayName("메시지 내용 확인")
    void generatedMessageShouldContainPayload() {
        generatorNode.generate("temperature", 25.5);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mockOutputPort).send(captor.capture());

        Message captured = captor.getValue();
        Assertions.assertTrue(captured.hasKey("temperature"));
        Assertions.assertEquals(25.5, captured.get("temperature"));
    }

    @Order(3)
    @Test
    @DisplayName("OutputPort 조회")
    void getOutputPortShouldNotBeNull() {
        Assertions.assertNotNull(generatorNode.getOutputPort());
    }

    @Order(4)
    @Test
    @DisplayName("다수 generate 호출")
    void multipleGenerateCallsShouldSendMessagesInOrder() {
        generatorNode.generate("key", "val1");
        generatorNode.generate("key", "val2");
        generatorNode.generate("key", "val3");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(mockOutputPort, times(3)).send(captor.capture());

        List<Message> captured = captor.getAllValues();
        Assertions.assertEquals("val1", captured.get(0).get("key"));
        Assertions.assertEquals("val2", captured.get(1).get("key"));
        Assertions.assertEquals("val3", captured.get(2).get("key"));
    }
}