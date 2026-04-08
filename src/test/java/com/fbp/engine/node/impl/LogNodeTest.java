package com.fbp.engine.node.impl;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;

import java.util.Map;

public class LogNodeTest {
    LogNode target;
    String targetId;
    Connection connModule;

    @BeforeEach
    void setUp(){
        targetId = "test";
        target = new LogNode(targetId);

        connModule = new Connection("conn-module");
        target.getOutputPort("out").connect(connModule);
    }

    @Order(1)
    @Test
    @DisplayName("메시지 통과 전달")
    void messagePassingForwarding(){
        target.process(new Message(Map.of("key", "value")));

        Assertions.assertNotNull(connModule.poll());
    }

    @Order(2)
    @Test
    @DisplayName("중간 삽입 가능")
    void intermediateInsertionPossible(){
        GeneratorNode testGen= new GeneratorNode("test-gen");
        Connection connection = new Connection("gen-to-log");
        testGen.getOutputPort().connect(connection);

        connection.setTarget(target.getInputPort("in"));

        testGen.generate("key","value");

        Message poll = connection.poll();
        Assertions.assertNotNull(poll);

        target.process(poll);

        Assertions.assertNotNull(connModule.poll());
    }
}
