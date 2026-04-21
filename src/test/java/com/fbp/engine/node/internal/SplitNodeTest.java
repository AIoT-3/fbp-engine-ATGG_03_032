package com.fbp.engine.core.node;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.internal.SplitNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.*;

public class SplitNodeTest {
    SplitNode splitNode;


    @BeforeEach
    void setUp(){
        splitNode = spy(new SplitNode("split", "test", 35));
    }

    @Order(1)
    @Test
    @DisplayName("조건 만족 -> match 포트")
    void conditionSatisfiedSendMatchPort(){
        Message message = new Message(Map.of("test", 36));

        splitNode.process("in", message);

        verify(splitNode, times(1)).send("match", message);
        verify(splitNode, never()).send("mismatch", message);
    }

    @Order(2)
    @Test
    @DisplayName("조건 미달 -> mismatch 포트")
    void conditionUnsatisfiedSendMismatchPort(){
        Message message = new Message(Map.of("test", 34));

        splitNode.process("in", message);

        verify(splitNode, never()).send("match", message);
        verify(splitNode, times(1)).send("mismatch", message);

    }

    @Order(3)
    @Test
    @DisplayName("양쪽 동시 확인")
    void checkBothSides(){
        Message matchedMessage = new Message(Map.of("test", 36));
        Message mismatchedMessage = new Message(Map.of("test", 34));

        splitNode.process("in",matchedMessage);
        splitNode.process("in",mismatchedMessage);

        verify(splitNode, times(1)).send("match", matchedMessage);
        verify(splitNode, times(1)).send("mismatch", mismatchedMessage);
    }

    @Order(2)
    @Test
    @DisplayName("경계값 처리")
    void checkBoundary(){
        Message message = new Message(Map.of("test", 35));

        splitNode.process("in",message);

        verify(splitNode, times(1)).send("match", message);
        verify(splitNode, never()).send("mismatch", message);
    }




}
