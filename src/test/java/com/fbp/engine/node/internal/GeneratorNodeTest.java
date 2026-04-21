package com.fbp.engine.node.internal;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class GeneratorNodeTest {
    private GeneratorNode generatorNode;

    Connection connection;

    @BeforeEach
    void setUp() {
        generatorNode = new GeneratorNode("gen-1");
        connection = new Connection("conn");
        generatorNode.getOutputPort("out").connect(connection);
    }

    @Order(1)
    @Test
    @DisplayName("generate 메시지 생성")
    void generateShouldSendMessageToOutputPort() {
        generatorNode.generate("key", "value");

        Assertions.assertNotNull(connection.poll());
    }

    @Order(2)
    @Test
    @DisplayName("메시지 내용 확인")
    void generatedMessageShouldContainPayload() {
        generatorNode.generate("temperature", 25.5);

        Message message = connection.poll();

        Assertions.assertEquals(25.5, message.get("temperature"));
    }

    @Order(3)
    @Test
    @DisplayName("OutputPort 조회")
    void getOutputPortShouldNotBeNull() {

        Assertions.assertNotNull(generatorNode.getOutputPort("out"));
    }

    @Order(4)
    @Test
    @DisplayName("다수 generate 호출")
    void multipleGenerateCallsShouldSendMessagesInOrder() {
        generatorNode.generate("key", "val1");
        generatorNode.generate("key", "val2");
        generatorNode.generate("key", "val3");

        Assertions.assertEquals("val1", connection.poll().get("key"));
        Assertions.assertEquals("val2", connection.poll().get("key"));
        Assertions.assertEquals("val3", connection.poll().get("key"));
    }
}