package com.fbp.engine.core.node;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.ErrorMessage;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ErrorHandlingTest {
    static class ThrowingNode extends AbstractNode {
        public ThrowingNode(String id) {
            super(id);
        }

        @Override
        public void onProcess(String portName, Message message) {
            throw new RuntimeException("Intentional Error for Testing");
        }
    }

    static class CapturingInputPort implements InputPort {
        Message receivedMessage = null;

        @Override
        public String getName() { return "in"; }

        @Override
        public void receive(Message message) {
            this.receivedMessage = message;
        }
    }

    @Test
    @DisplayName("에러 발생 시 분기: process()에서 예외 → 에러 포트로 메시지 전달")
    void testErrorBranching() {
        ThrowingNode node = new ThrowingNode("test-node");
        Connection errorConnection = new Connection("err-conn");
        CapturingInputPort errorInPort = new CapturingInputPort();
        errorConnection.setTarget(errorInPort);
        
        node.getOutputPort("_error").connect(errorConnection);

        Message originalMessage = new Message(Map.of("data", "test payload"));

        node.process("in", originalMessage);
        
        Message deliveredMessage = errorConnection.poll();
        
        assertNotNull(deliveredMessage);
        assertTrue(deliveredMessage instanceof ErrorMessage);
    }

    @Test
    @DisplayName("에러 메시지 내용: 원본 메시지, 예외 정보, 노드 id 포함")
    void testErrorMessageContent() {
        ThrowingNode node = new ThrowingNode("test-node");
        Connection errorConnection = new Connection("err-conn");
        node.getOutputPort("_error").connect(errorConnection);

        Message originalMessage = new Message(Map.of("data", "test payload"));
        node.process("in", originalMessage);
        
        ErrorMessage errorMsg = (ErrorMessage) errorConnection.poll();
        
        assertEquals(originalMessage, errorMsg.getOriginalMessage());
        assertTrue(errorMsg.getException() instanceof RuntimeException);
        assertEquals("Intentional Error for Testing", errorMsg.getException().getMessage());
        assertEquals("test-node", errorMsg.getErrorNodeId());
    }

    @Test
    @DisplayName("에러 포트 미연결: 로그 기록 후 계속 (예외 발생 안 함)")
    void testErrorPortNotConnected() {
        ThrowingNode node = new ThrowingNode("test-node");

        Message originalMessage = new Message(Map.of("data", "test payload"));
        
        assertDoesNotThrow(() -> {
            node.process("in", originalMessage);
        });
    }

    @Test
    @DisplayName("정상 처리 시: 예외 없으면 에러 포트에 메시지 전달하지 않음")
    void testNormalProcessing() {
        AbstractNode normalNode = new AbstractNode("normal") {
            @Override
            public void onProcess(String portName, Message message) {
            }
        };
        Connection errorConnection = new Connection("err-conn");
        normalNode.getOutputPort("_error").connect(errorConnection);

        normalNode.process("in", new Message(Map.of("data", "test")));
        
        assertEquals(0, errorConnection.getBufferSize());
    }
}
