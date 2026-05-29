package com.fbp.engine.core.parser;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@AllArgsConstructor
@NoArgsConstructor
public class FlowDefinition {
    private String id;
    private String name;
    private String description;

    private List<NodeDefinition> nodes;
    private List<ConnectionDefinition> connections;
    
    private Map<String, String> publicPorts;

    public FlowDefinition(String id, String name, String description, List<NodeDefinition> nodes, List<ConnectionDefinition> connections) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.nodes = nodes;
        this.connections = connections;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setNodes(List<NodeDefinition> nodes) {
        this.nodes = nodes;
    }

    public void setConnections(List<ConnectionDefinition> connections) {
        this.connections = connections;
    }

    public Map<String, String> getPublicPorts() {
        return publicPorts;
    }

    public void setPublicPorts(Map<String, String> publicPorts) {
        this.publicPorts = publicPorts;
    }

    public List<NodeDefinition> getNodes() {
        return nodes == null ? new ArrayList<>() : new ArrayList<>(nodes);
    }

    public List<ConnectionDefinition> getConnections() {
        return connections == null ? new ArrayList<>() : new ArrayList<>(connections);
    }

    public NodeDefinition getNode(String id){
        if (nodes == null) return null;
        
        Optional<NodeDefinition> first = nodes.stream().filter(nodeDefinition -> nodeDefinition.getId().equals(id)).findFirst();

        if(first.isPresent()){
            return first.get();
        }
        return null;
    }
}
