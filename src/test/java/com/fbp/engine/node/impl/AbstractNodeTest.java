package com.fbp.engine.node.impl;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;

import java.util.Map;

public class AbstractNodeTest {

    AbstractNode testNode;
    String testId;
    boolean onProcessed;

    @BeforeEach
    void setUp(){
        testId = "test";
        testNode = new AbstractNode(testId) {
            @Override
            public void onProcess(Message message) {
                onProcessed = true;
            }
        };
        onProcessed = false;
    }

    @Order(1)
    @Test
    @DisplayName("getId 반환")
    void returnGetId(){
        Assertions.assertEquals(testId,testNode.getId());
    }

    @Order(2)
    @Test
    @DisplayName("addInputPort 등록")
    void submitAddInputPort(){
        testNode.addInputPort("in");
        Assertions.assertNotNull(testNode.getInputPort("in"));
    }

    @Order(3)
    @Test
    @DisplayName("addOutputPort 등록")
    void submitAddOutputPort(){
        testNode.addOutputPort("out");
        Assertions.assertNotNull(testNode.getOutputPort("out"));
    }

    @Order(4)
    @Test
    @DisplayName("미등록 포트 조회")
    void checkUnregisteredPort(){
        Assertions.assertAll(
                () -> Assertions.assertNull(testNode.getInputPort("없는 포트")),
                () -> Assertions.assertNull(testNode.getOutputPort("없는 포트"))
        );
    }

    @Order(5)
    @Test
    @DisplayName("process -> onProcess 호출")
    void processShouldCallOnProcess(){
        testNode.process(new Message(Map.of("noMeans", "value")));

        Assertions.assertTrue(onProcessed);
    }

    @Order(6)
    @Test
    @DisplayName("send로 메시지 전달")
    void messageSend(){
        testNode.addOutputPort("out");

        Connection connection = new Connection("test-conn");
        testNode.getOutputPort("out").connect(connection);

        testNode.send("out", new Message(
                Map.of("key", "value")
        ));

        Assertions.assertNotNull(connection.poll());
    }
}
