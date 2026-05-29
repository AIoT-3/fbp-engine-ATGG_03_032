package com.fbp.engine.node.routing;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;

class DynamicRouterNodeTest {

    private TestableDynamicRouterNode router;
    private CapturingInputPort portA;
    private CapturingInputPort portB;
    private CapturingInputPort defaultPort;

    // Helper class to expose protected methods for testing
    static class TestableDynamicRouterNode extends DynamicRouterNode {
        public TestableDynamicRouterNode(String id, String defaultPort) {
            super(id, defaultPort);
        }

        public void publicAddOutputPort(String name) {
            if (getOutputPort(name) == null) {
                addOutputPort(name);
            }
        }
    }

    // Helper class to capture messages for testing
    static class CapturingInputPort implements InputPort {
        Message receivedMessage = null;

        @Override
        public String getName() { return "in"; }

        @Override
        public void receive(Message message) {
            this.receivedMessage = message;
        }
        
        public void clear() {
            receivedMessage = null;
        }
    }

    @BeforeEach
    void setUp() {
        router = new TestableDynamicRouterNode("router", "default-out");
        portA = new CapturingInputPort();
        portB = new CapturingInputPort();
        defaultPort = new CapturingInputPort();

        // Connect ports for testing using a mock connection
        connect(router, "portA", portA);
        connect(router, "portB", portB);
        connect(router, "default-out", defaultPort);
    }

    private void connect(TestableDynamicRouterNode node, String portName, InputPort targetPort) {
        node.publicAddOutputPort(portName);
        
        // Use a mock connection that calls receive() directly in deliver()
        Connection mockConn = Mockito.mock(Connection.class);
        doAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            targetPort.receive(msg);
            return null;
        }).when(mockConn).deliver(Mockito.any(Message.class));
        
        node.getOutputPort(portName).connect(mockConn);
    }

    @Test
    @DisplayName("조건 매칭: 메시지 필드 값에 따라 올바른 출력 포트로 전달")
    void testRuleMatching() {
        router.addRule(new RoutingRule("type", "equals", "temp", "portA"));
        router.addRule(new RoutingRule("type", "equals", "humidity", "portB"));

        Message tempMessage = new Message(Map.of("type", "temp", "value", 25));
        router.onProcess("in", tempMessage);
        
        assertNotNull(portA.receivedMessage);
        assertNull(portB.receivedMessage);
        assertNull(defaultPort.receivedMessage);
        assertEquals(tempMessage, portA.receivedMessage);
    }

    @Test
    @DisplayName("다중 규칙: 여러 RoutingRule 중 첫 매칭 규칙의 포트로 전달")
    void testFirstMatchWins() {
        router.addRule(new RoutingRule("value", "gt", 20, "portA"));
        router.addRule(new RoutingRule("value", "gt", 10, "portB")); // This also matches, but should be ignored

        Message message = new Message(Map.of("value", 25));
        router.onProcess("in", message);

        assertNotNull(portA.receivedMessage);
        assertNull(portB.receivedMessage);
        assertNull(defaultPort.receivedMessage);
    }

    @Test
    @DisplayName("기본 포트: 어떤 규칙도 매칭되지 않으면 default 포트로 전달")
    void testDefaultPort() {
        router.addRule(new RoutingRule("type", "equals", "temp", "portA"));
        
        Message otherMessage = new Message(Map.of("type", "pressure"));
        router.onProcess("in", otherMessage);

        assertNull(portA.receivedMessage);
        assertNull(portB.receivedMessage);
        assertNotNull(defaultPort.receivedMessage);
    }

    @Test
    @DisplayName("규칙 없음: 규칙이 비어 있으면 모든 메시지가 default로 전달")
    void testNoRules() {
        Message message = new Message(Map.of("value", 123));
        router.onProcess("in", message);

        assertNull(portA.receivedMessage);
        assertNull(portB.receivedMessage);
        assertNotNull(defaultPort.receivedMessage);
    }

    @Test
    @DisplayName("null 필드: 라우팅 필드가 메시지에 없으면 default 포트")
    void testMissingField() {
        router.addRule(new RoutingRule("type", "equals", "temp", "portA"));
        
        Message message = new Message(Map.of("another_field", "some_value"));
        router.onProcess("in", message);

        assertNull(portA.receivedMessage);
        assertNotNull(defaultPort.receivedMessage);
    }

    @Test
    @DisplayName("런타임 규칙 변경: 실행 중 규칙 추가/제거 가능")
    void testRuntimeRuleChange() {
        Message message = new Message(Map.of("type", "temp"));
        
        // Initially, no rules, goes to default
        router.onProcess("in", message);
        assertNotNull(defaultPort.receivedMessage);
        
        // Add a rule
        portA.clear();
        defaultPort.clear();
        RoutingRule rule = new RoutingRule("type", "equals", "temp", "portA");
        router.addRule(rule);
        router.onProcess("in", message);
        assertNotNull(portA.receivedMessage);
        assertNull(defaultPort.receivedMessage);
        
        // Remove the rule
        portA.clear();
        defaultPort.clear();
        router.removeRule(rule);
        router.onProcess("in", message);
        assertNull(portA.receivedMessage);
        assertNotNull(defaultPort.receivedMessage);
    }
}
