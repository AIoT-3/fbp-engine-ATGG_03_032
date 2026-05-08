package com.fbp.engine.core.parser;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
public class FlowDefinition {
    private String id;
    private String name;
    private String description;

    private List<NodeDefinition> nodes;
    private List<ConnectionDefinition> connections;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<NodeDefinition> getNodes() {
        return new ArrayList<>(nodes);
    }

    public List<ConnectionDefinition> getConnections() {
        return new ArrayList<>(connections);
    }

    public NodeDefinition getNode(String id){
        Optional<NodeDefinition> first = nodes.stream().filter(nodeDefinition -> nodeDefinition.getId().equals(id)).findFirst();

        if(first.isPresent()){
            return first.get();
        }
        return null;
    }
}