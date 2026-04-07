package com.fbp.engine.node.impl;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class PrintNodeTest {
    private String testId;
    PrintNode printNode;

    private final PrintStream standardOut = System.out;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();


    @BeforeEach
    void setUp(){
        testId = "test-printer";
        printNode = new PrintNode(testId);
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @Order(1)
    @Test
    @DisplayName("getId 반환")
    void returnGetId(){
        Assertions.assertEquals(testId, printNode.getId());
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

    @Order(4)
    @Test
    @DisplayName("InputPort 조회")
    void getInputPort(){
        Assertions.assertNotNull(printNode.getInputPort());
    }

    @Order(5)
    @Test
    @DisplayName("InputPort를 통한 수신")
    void UsingInputPortForReceiving(){
        Map<String, Object> payload = new HashMap<>();
        payload.put("temperature", 25.5);
        Message test = new Message(payload);

        printNode.getInputPort().receive(test);

        String output = outputStreamCaptor.toString();
        Assertions.assertTrue(output.contains("test-printer"));
        Assertions.assertTrue(output.contains("temperature"));
    }

}
