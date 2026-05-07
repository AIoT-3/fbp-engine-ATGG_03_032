package com.fbp.engine.registry;

import com.fbp.engine.core.node.Node;
import com.fbp.engine.core.node.ProtocolNode;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.internal.CollectorNode;
import com.fbp.engine.node.internal.LogNode;
import com.fbp.engine.node.internal.PrintNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;
import java.util.Set;

public class NodeRegistryTest {
    @Order(1)
    @Test
    @DisplayName("register + create")
    void registerAndCreateTest(){
        NodeFactory logNodeFactory = (id, config) -> {
            return new LogNode(id);
        };

        NodeRegistry nodeRegistry = new NodeRegistry();
        nodeRegistry.register("logNode", logNodeFactory);
        Node logNode = nodeRegistry.create("logNode", null);

        Assertions.assertNotNull(logNode);
        Assertions.assertTrue(logNode instanceof LogNode);
    }

    @Order(2)
    @Test
    @DisplayName("미등록 타입 create")
    void checkCreateUnregisteredTypeCase(){
        NodeRegistry nodeRegistry = new NodeRegistry();

        Assertions.assertThrows(NodeRegistryException.class, ()->{
            nodeRegistry.create("unknownType", null);
        });
    }

    @Order(3)
    @Test
    @DisplayName("중복 등록 처리")
    void checkDuplicateRegistrationProcessing(){
        NodeFactory logNodeFactory = (id, config) -> {
            return new LogNode(id);
        };

        NodeRegistry nodeRegistry = new NodeRegistry();
        nodeRegistry.register("logNode", logNodeFactory);

        Assertions.assertThrows(NodeRegistryException.class, ()->{
            nodeRegistry.register("logNode", logNodeFactory);
        });
    }

    @Order(4)
    @Test
    @DisplayName("getRegisteredTypes")
    void getRegisteredTypesMethodTest(){
        NodeFactory logNodeFactory = (id, config) -> {
            return new LogNode(id);
        };
        NodeFactory printNodeFactory = (id, config) -> {
            return new PrintNode(id);
        };
        NodeFactory collectorNodeFactory = (id, config) -> {
            return new CollectorNode(id);
        };

        NodeRegistry nodeRegistry = new NodeRegistry();
        nodeRegistry.register("logNode", logNodeFactory);
        nodeRegistry.register("printNode", printNodeFactory);
        nodeRegistry.register("collectorNode", collectorNodeFactory);

        Set<String> registeredTypes = nodeRegistry.getRegisteredTypes();
        Assertions.assertAll(
                ()->Assertions.assertTrue(registeredTypes.contains("logNode")),
                ()->Assertions.assertTrue(registeredTypes.contains("printNode")),
                ()->Assertions.assertTrue(registeredTypes.contains("collectorNode"))
        );
    }

    @Order(5)
    @Test
    @DisplayName("config 전달")
    void checkPassConfig(){
        NodeFactory protocolNodeFactory = (id, config) -> {
            return new ProtocolNode(id, config) {
                @Override
                protected void connect() throws Exception {return;}
                @Override
                protected void disconnect() {return;}
                @Override
                public void onProcess(String portName, Message message) {return;}
            };
        };

        NodeRegistry nodeRegistry = new NodeRegistry();
        nodeRegistry.register("protocolNode", protocolNodeFactory);

        Map<String, Object> config = Map.of("ka","va",
                "kb","vb",
                "kc","vc");

        Node protocolNode = nodeRegistry.create("protocolNode", config);

        Assertions.assertTrue(protocolNode instanceof ProtocolNode);

        ProtocolNode pN = (ProtocolNode) protocolNode;

        Assertions.assertTrue(
                pN.getConfig("ka").equals("va")&&
                pN.getConfig("kb").equals("vb")&&
                pN.getConfig("kc").equals("vc")
        );
    }

    @Order(6)
    @Test
    void isRegisteredMethodTest(){
        NodeFactory logNodeFactory = (id, config) -> {
            return new LogNode(id);
        };
        NodeFactory printNodeFactory = (id, config) -> {
            return new PrintNode(id);
        };
        NodeFactory collectorNodeFactory = (id, config) -> {
            return new CollectorNode(id);
        };

        NodeRegistry nodeRegistry = new NodeRegistry();
        nodeRegistry.register("logNode", logNodeFactory);
        nodeRegistry.register("printNode", printNodeFactory);
        nodeRegistry.register("collectorNode", collectorNodeFactory);

        Assertions.assertAll(
                ()->Assertions.assertTrue(nodeRegistry.isRegistered("logNode")),
                ()-> Assertions.assertTrue(nodeRegistry.isRegistered("printNode")),
                ()->Assertions.assertTrue(nodeRegistry.isRegistered("collectorNode")),
                ()->Assertions.assertFalse(nodeRegistry.isRegistered("unknown"))
        );
    }

    @Order(7)
    @Test
    @DisplayName("null/빈 타입명")
    void whenMethodCallParameterNullStringOrBlankString(){
        NodeRegistry nodeRegistry = new NodeRegistry();

        Assertions.assertAll(
                ()->Assertions.assertThrows(NodeRegistryException.class,
                        ()->{nodeRegistry.create("", Map.of());}),
                ()->Assertions.assertThrows(NodeRegistryException.class,
                    ()->{nodeRegistry.create(null,Map.of());}),
                ()->Assertions.assertThrows(NodeRegistryException.class,
                        ()->nodeRegistry.isRegistered("")),
                ()->Assertions.assertThrows(NodeRegistryException.class,
                        ()->nodeRegistry.isRegistered(null))
        );
    }
}
