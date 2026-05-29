package com.fbp.engine.node.flow;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.core.parser.ConnectionDefinition;
import com.fbp.engine.core.parser.FlowDefinition;
import com.fbp.engine.core.parser.NodeDefinition;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.core.registry.NodeRegistry;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;

class SubFlowNodeTest {

    private NodeRegistry registry;
    private SubFlowNode subFlowNode;
    private CapturingInputPort externalOutPort;

    static class PassthroughNode extends AbstractNode {
        public PassthroughNode(String id) {
            super(id);
            addInputPort("in");
            addOutputPort("out");
        }

        @Override
        public void onProcess(String portName, Message message) {
            send("out", message);
        }
    }

    static class ErrorNode extends AbstractNode {
        public ErrorNode(String id) {
            super(id);
            addInputPort("in");
            addOutputPort("out");
        }

        @Override
        public void onProcess(String portName, Message message) {
            throw new RuntimeException("Internal Subflow Error");
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

    @BeforeEach
    void setUp() {
        registry = new NodeRegistry();
        registry.register("Passthrough", (id, config) -> new PassthroughNode(id));
        registry.register("ErrorNode", (id, config) -> new ErrorNode(id));

        NodeDefinition node1 = new NodeDefinition("node1", "Passthrough", Collections.emptyMap());
        NodeDefinition node2 = new NodeDefinition("node2", "Passthrough", Collections.emptyMap());
        ConnectionDefinition conn1 = new ConnectionDefinition("node1:out", "node2:in");
        
        FlowDefinition subFlowDef = new FlowDefinition(
                "sub1", "SubFlow", "",
                List.of(node1, node2),
                List.of(conn1)
        );
        subFlowDef.setPublicPorts(Map.of(
                "public_in", "node1.in",
                "public_out", "node2.out"
        ));

        subFlowNode = new SubFlowNode("subflow-node", subFlowDef, registry);

        externalOutPort = new CapturingInputPort();
        Connection mockConn = Mockito.mock(Connection.class);
        doAnswer(invocation -> {
            Message msg = invocation.getArgument(0);
            externalOutPort.receive(msg);
            return null;
        }).when(mockConn).deliver(Mockito.any(Message.class));
        subFlowNode.getOutputPort("public_out").connect(mockConn);
    }
    
    @AfterEach
    void tearDown() {
        if (subFlowNode != null) {
            subFlowNode.shutdown();
        }
    }

    @Test
    @DisplayName("메시지 전달: 외부 입력 → 서브플로우 내부 → 외부 출력 정상 전달")
    void testMessagePassing() throws InterruptedException {
        subFlowNode.initialize();
        
        Message message = new Message(Map.of("data", "test"));
        subFlowNode.process("public_in", message);

        Thread.sleep(100);
        
        assertNotNull(externalOutPort.receivedMessage);
        assertEquals(message, externalOutPort.receivedMessage);
    }

    @Test
    @DisplayName("내부 에러 전파: 서브플로우 내부에서 에러 발생 시 외부 에러 포트로 전파")
    void testInternalErrorPropagation() throws InterruptedException {
        NodeDefinition node1 = new NodeDefinition("err_node", "ErrorNode", Collections.emptyMap());
        FlowDefinition subFlowDef = new FlowDefinition("sub2", "ErrFlow", "", List.of(node1), Collections.emptyList());
        subFlowDef.setPublicPorts(Map.of("public_in", "err_node.in"));
        
        SubFlowNode errorSubFlowNode = new SubFlowNode("err-subflow", subFlowDef, registry);

        CapturingInputPort errorPort = new CapturingInputPort();
        Connection mockConn = Mockito.mock(Connection.class);
        doAnswer(invocation -> {
            errorPort.receive(invocation.getArgument(0));
            return null;
        }).when(mockConn).deliver(Mockito.any(Message.class));
        errorSubFlowNode.getOutputPort("_error").connect(mockConn);
        
        errorSubFlowNode.initialize();
        
        errorSubFlowNode.process("public_in", new Message(Map.of("data", "test")));
        
        Thread.sleep(100);
        
        assertNotNull(errorPort.receivedMessage);
        assertEquals("err_node", ((com.fbp.engine.message.ErrorMessage)errorPort.receivedMessage).getErrorNodeId());
        
        errorSubFlowNode.shutdown();
    }
}
