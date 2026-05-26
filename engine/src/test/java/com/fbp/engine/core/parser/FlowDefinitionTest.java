package com.fbp.engine.core.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FlowDefinitionTest {

    @Order(1)
    @Test
    @DisplayName("불변성")
    void checkImmutability(){
        FlowDefinition flowDefinition = new FlowDefinition("id", "name", "description", new ArrayList<>(), new ArrayList<>());
        flowDefinition.getNodes().add(new NodeDefinition());
        flowDefinition.getConnections().add(new ConnectionDefinition());

        Assertions.assertAll(
                ()->Assertions.assertTrue(flowDefinition.getNodes().isEmpty()),
                ()->Assertions.assertTrue(flowDefinition.getConnections().isEmpty())
        );
    }

    @Order(2)
    @Test
    @DisplayName("노드 조회")
    void checkFindNodeById(){
        List<NodeDefinition> nodeDefinitions = new ArrayList<>();
        nodeDefinitions.add(new NodeDefinition("id", "type", Map.of()));
        FlowDefinition flowDefinition = new FlowDefinition("id", "name", "description", nodeDefinitions, new ArrayList<>());

        Assertions.assertNotNull(flowDefinition.getNode("id"));
    }

    @Order(3)
    @Test
    @DisplayName("모든 연결이 존재 하는 노드를 참조 하는지 검증")
    void checkConnectionReferenceDefinedNode(){
        List<NodeDefinition> nodes = List.of(
                new NodeDefinition("source-1", "http-in", Map.of()),
                new NodeDefinition("sink-1", "log", Map.of())
        );

        ConnectionDefinition conn = new ConnectionDefinition();
        conn.setFrom("source-1:out");
        conn.setTo("sink-1:in");

        FlowDefinition flowDefinition = new FlowDefinition("f1", "flow", "desc", nodes, List.of(conn));

        for (ConnectionDefinition c : flowDefinition.getConnections()) {
            String fromNodeId = c.getFrom().split(":")[0];
            String toNodeId = c.getTo().split(":")[0];

            Assertions.assertAll(
                    () -> Assertions.assertNotNull(flowDefinition.getNode(fromNodeId)),
                    () -> Assertions.assertNotNull(flowDefinition.getNode(toNodeId))
            );
        }
    }
}
