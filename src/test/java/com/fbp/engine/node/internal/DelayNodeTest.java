package com.fbp.engine.node.internal;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;

import java.util.Map;

public class DelayNodeTest {
    DelayNode delayNode;
    Connection connection;
    Message message;

    @BeforeEach
    void setUp(){
        delayNode = new DelayNode("delay", 500);
        connection = new Connection("conn");
        delayNode.getOutputPort("out").connect(connection);

        message = new Message(Map.of("test","value"));
    }

    @Order(1)
    @Test
    @DisplayName("지연 후 전달")
    void deliveryAfterDelay(){
        long start = System.currentTimeMillis();
        delayNode.process("in", message);
        connection.poll();
        long end = System.currentTimeMillis()-start;

        Assertions.assertTrue(end>450);
    }

    @Order(2)
    @Test
    @DisplayName("메시지 내용 보존")
    void preserveMessageContent(){
        delayNode.process("in", message);
        Message poll = connection.poll();
        Assertions.assertEquals(message, poll);
    }
}
