package com.fbp.engine.node.impl;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;

import java.util.Map;

public class CounterNodeTest {
    CounterNode counterNode;
    Connection connection;

    Message message;

    @BeforeEach
    void setUp(){
        counterNode = new CounterNode("counter");
        connection = new Connection("conn");
        counterNode.getOutputPort("out").connect(connection);

        message = new Message(Map.of("test1","value1", "test2", "value2", "test3", "value3"));
    }

    @Order(1)
    @Test
    @DisplayName("count 키 추가")
    void whenSendThenIncreaseCount(){
        counterNode.process("in",message);

        Message poll = connection.poll();

        Assertions.assertEquals(1l, (long) poll.get("count"));
    }

    @Order(2)
    @Test
    @DisplayName("count 누적")
    void countCumulative(){
        counterNode.process("in", message);
        counterNode.process("in", message);
        counterNode.process("in", message);

        connection.poll();
        connection.poll();
        Message poll = connection.poll();

        Assertions.assertEquals(3l, (long) poll.get("count"));
    }

    @Order(3)
    @Test
    @DisplayName("원본 키 유지")
    void keepOriginalKey(){
        counterNode.process("in", message);

        Message poll = connection.poll();

        Assertions.assertAll(
                () -> Assertions.assertEquals((String)message.get("test1"), poll.get("test1")),
                () -> Assertions.assertEquals((String)message.get("test2"), poll.get("test2")),
                () -> Assertions.assertEquals((String)message.get("test3"), poll.get("test3"))
        );
    }
}
