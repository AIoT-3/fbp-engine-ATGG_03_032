package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;
import org.junit.jupiter.api.*;

import java.util.Map;

public class PrintNodeTest {
    PrintNode printNode;

    @BeforeEach
    void setUp(){
        printNode = new PrintNode("test-printer");
    }

    @Order(1)
    @Test
    @DisplayName("getId 반환")
    void returnGetId(){
        Assertions.assertEquals("test-printer", printNode.getId());
    }

    @Order(2)
    @Test
    @DisplayName("process 정상 동작")
    void successfullyProcess(){
        Assertions.assertDoesNotThrow(
                () -> printNode.process(new Message(
                        Map.of("test","value")
                ))
        );
    }

    @Order(3)
    @Test
    @DisplayName("Node 인터페이스 구현")
    void NodeInterfaceImplement(){
        Assertions.assertDoesNotThrow(
                () -> {
                    Node node = printNode;
                }
        );
    }
}
