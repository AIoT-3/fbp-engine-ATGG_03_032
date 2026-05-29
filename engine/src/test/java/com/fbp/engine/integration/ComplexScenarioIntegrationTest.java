package com.fbp.engine.integration;

import com.fbp.engine.core.engine.FlowManager;
import com.fbp.engine.core.parser.ConnectionDefinition;
import com.fbp.engine.core.parser.FlowDefinition;
import com.fbp.engine.core.parser.NodeDefinition;
import com.fbp.engine.node.routing.DynamicRouterNode;
import com.fbp.engine.node.routing.RoutingRule;
import com.fbp.engine.node.flow.SubFlowNode;
import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.message.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class ComplexScenarioIntegrationTest {

    private FlowManager flowManager;
    private final ObjectMapper mapper = new ObjectMapper();

    static class DummyNode extends AbstractNode {
        private final String outPort;
        CountDownLatch latch;
        Message lastMessage;

        public DummyNode(String id, String outPort) {
            super(id);
            this.outPort = outPort;
            addInputPort("in");
            if (outPort != null) addOutputPort(outPort);
        }

        public void setLatch(CountDownLatch latch) { this.latch = latch; }

        @Override
        public void onProcess(String portName, Message message) {
            this.lastMessage = message;
            if (outPort != null) send(outPort, message);
            if (latch != null) latch.countDown();
        }
    }
    
    static class MqttInNode extends AbstractNode {
        public MqttInNode(String id) {
            super(id);
            addInputPort("in");
            addOutputPort("out");
        }
        @Override
        public void onProcess(String portName, Message message) {
            send("out", message);
        }
    }

    @BeforeEach
    void setUp() {
        flowManager = FlowManager.getInstance();
        flowManager.reset();
        
        flowManager.getNodeRegistry().register("DynamicRouter", (id, config) -> {
            DynamicRouterNode router = new DynamicRouterNode(id, "default");
            if (config.containsKey("rules")) {
                List<Map<String, Object>> rules = (List<Map<String, Object>>) config.get("rules");
                for (Map<String, Object> r : rules) {
                    router.addRule(new RoutingRule((String)r.get("field"), (String)r.get("op"), r.get("val"), (String)r.get("port")));
                }
            }
            return router;
        });
        
        flowManager.getNodeRegistry().register("MqttIn", (id, config) -> new MqttInNode(id));
        flowManager.getNodeRegistry().register("Dummy", (id, config) -> new DummyNode(id, "out"));
        flowManager.getNodeRegistry().register("Sink", (id, config) -> new DummyNode(id, null));
    }

    @AfterEach
    void tearDown() {
        flowManager.reset();
    }

    @Test
    @DisplayName("복합 시나리오 1: 동적 라우팅 및 다중 규칙 분기")
    void testComplexScenario() throws InterruptedException {
        NodeDefinition mqttIn = new NodeDefinition("mqttIn", "MqttIn", Collections.emptyMap());
        
        Map<String, Object> routerConfig = Map.of("rules", List.of(
            Map.of("field", "sensor", "op", "equals", "val", "temp", "port", "tempPort"),
            Map.of("field", "sensor", "op", "equals", "val", "humid", "port", "humidPort")
        ));
        NodeDefinition router = new NodeDefinition("router", "DynamicRouter", routerConfig);
        
        NodeDefinition tempSink = new NodeDefinition("tempSink", "Sink", Collections.emptyMap());
        NodeDefinition humidSink = new NodeDefinition("humidSink", "Sink", Collections.emptyMap());
        NodeDefinition defaultSink = new NodeDefinition("defaultSink", "Sink", Collections.emptyMap());

        FlowDefinition flowDef = new FlowDefinition(
            "complex-flow", "Test Flow", "",
            List.of(mqttIn, router, tempSink, humidSink, defaultSink),
            List.of(
                new ConnectionDefinition("mqttIn:out", "router:in"),
                new ConnectionDefinition("router:tempPort", "tempSink:in"),
                new ConnectionDefinition("router:humidPort", "humidSink:in"),
                new ConnectionDefinition("router:default", "defaultSink:in")
            )
        );

        flowManager.deploy(flowDef);
        
        AbstractNode entryNode = flowManager.getNode("complex-flow", "mqttIn");
        DummyNode tempSinkNode = (DummyNode) flowManager.getNode("complex-flow", "tempSink");
        DummyNode humidSinkNode = (DummyNode) flowManager.getNode("complex-flow", "humidSink");
        
        CountDownLatch latch1 = new CountDownLatch(1);
        tempSinkNode.setLatch(latch1);
        
        entryNode.process("in", new Message(Map.of("sensor", "temp", "value", 25)));
        assertTrue(latch1.await(1, TimeUnit.SECONDS));
        assertNotNull(tempSinkNode.lastMessage);
        
        CountDownLatch latch2 = new CountDownLatch(1);
        humidSinkNode.setLatch(latch2);
        
        entryNode.process("in", new Message(Map.of("sensor", "humid", "value", 80)));
        assertTrue(latch2.await(1, TimeUnit.SECONDS));
        assertNotNull(humidSinkNode.lastMessage);
    }
}
