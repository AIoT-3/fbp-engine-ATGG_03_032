package com.fbp.engine.core.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonFlowParser implements FlowParser {
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public FlowDefinition parse(InputStream inputStream) {
        try {
            FlowDefinition flowDefinition = mapper.readValue(inputStream, FlowDefinition.class);
            validateFlowDefinition(flowDefinition);
            return flowDefinition;
        } catch (Exception e) {
            throw new FlowParserException(e);
        }
    }

    private void validateFlowDefinition(FlowDefinition flowDefinition) {
        validateBase(flowDefinition);
        validateNodeDefinitions(flowDefinition);
        validateConnectionDefinitions(flowDefinition);
    }

    private void validateBase(FlowDefinition flowDefinition) {
        if (flowDefinition == null) {
            throw new IllegalArgumentException("flowDefinition must be notNull");
        }
        if (flowDefinition.getId() == null) {
            throw new IllegalArgumentException("flowDefinition must be contain flowId");
        }
    }

    private void validateNodeDefinitions(FlowDefinition flowDefinition) {
        List<NodeDefinition> nodeDefinitions = flowDefinition.getNodes();

        if (nodeDefinitions == null) {
            throw new IllegalArgumentException("nodeDefinitions must be notNull");
        }
        if (nodeDefinitions.isEmpty()) {
            throw new IllegalArgumentException("nodeDefinitions must be contain least one node");
        }

        Set<String> alreadySeen = new HashSet<>();
        for (NodeDefinition node : nodeDefinitions) {
            if (!alreadySeen.add(node.getId())) {
                throw new IllegalArgumentException("duplicate nodeDefinition id found:" + node.getId());
            }
        }
    }

    private void validateConnectionDefinitions(FlowDefinition flowDefinition){
        List<ConnectionDefinition> connections = flowDefinition.getConnections();
        if(connections != null && !connections.isEmpty()) {
            Set<String> nodeIds = flowDefinition.getNodes().stream()
                    .map(NodeDefinition::getId)
                    .collect(Collectors.toSet());

            for(ConnectionDefinition connectionDefinition: connections){
                String from = connectionDefinition.getFrom();
                String to = connectionDefinition.getTo();

                if(from == null || to == null){
                    throw new IllegalArgumentException("connectionDefinition must be contain from and to");
                }

                String[] fromSplit = from.trim().split(":");
                String[] toSplit = to.trim().split(":");

                if(fromSplit.length != 2 || toSplit.length != 2 ||
                fromSplit[0].isBlank() || fromSplit[1].isBlank() ||
                toSplit[0].isBlank() || toSplit[1].isBlank()){
                    throw new IllegalArgumentException(
                            "connectionDefinition invalid format... from:" + from + " to:" + to);
                }

                if(!nodeIds.contains(fromSplit[0])){
                    throw new IllegalArgumentException(
                            "connectionDefinition must be reference defined nodeId... id:" + fromSplit[0]
                    );
                }
                if(!nodeIds.contains(toSplit[0])){
                    throw new IllegalArgumentException(
                            "connectionDefinition must be reference defined nodeId... id:" + toSplit[0]
                    );
                }
            }

        }
    }
}