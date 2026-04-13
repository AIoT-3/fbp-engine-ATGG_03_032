package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class TransformNodeTest {
    private Message mockMessage;
    private Message transformedMessage;

    @BeforeEach
    void setUp() {
        mockMessage = mock(Message.class);
        transformedMessage = mock(Message.class);
    }

    @Order(1)
    @Test
    @DisplayName("변환 정상 동작")
    void transformingSuccessfully(){
        TransformNode transformNode = spy(new TransformNode("tra", message -> transformedMessage));

        transformNode.process("in", mockMessage);

        verify(transformNode, times(1)).send("out", transformedMessage);
    }

    @Order(2)
    @Test
    @DisplayName("null 반환 시 미전달")
    void whenNullReturnThenNotSend(){
        TransformNode transformNode = spy((new TransformNode("tra", message -> null)));

        transformNode.process("in", mockMessage);

        verify(transformNode, never()).send(any(),any());
    }

    @Order(3)
    @Test
    @DisplayName("원본 메시지 불변")
    void originalMessageImmutable(){
        Message original = new Message(Map.of("test","original"));

        TransformNode node = new TransformNode("tra", msg -> {
            return msg.withEntry("test", "new");
        });

        node.onProcess("in", original);

        assertEquals("original",original.get("test"));
    }
}
