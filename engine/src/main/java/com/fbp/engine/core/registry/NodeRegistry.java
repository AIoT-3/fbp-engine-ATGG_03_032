package com.fbp.engine.core.registry;

import com.fbp.engine.core.node.Node;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class NodeRegistry {
    private final Map<String, NodeFactory> nodeFactories = new HashMap<>();

    public void register(String typeName, NodeFactory factory){
        try {
            if (typeName == null || typeName.isBlank()) {
                throw new IllegalArgumentException("typeName must be notBlank");
            }
            if (factory == null) {
                throw new IllegalArgumentException("factory must be notNull");
            }
            if(nodeFactories.containsKey(typeName)){
                throw new NodeRegistryException("typeName:" + typeName + " factory already exists");
            }
            nodeFactories.put(typeName, factory);
        }catch (NodeRegistryException e){
          throw e;
        } catch (Exception e) {
            throw new NodeRegistryException(e);
        }
    }

    public Node create(String id, String typeName, Map<String, Object> config){
        try {
            if (typeName == null || typeName.isBlank()) {
                throw new IllegalArgumentException("typeName must be notBlank");
            }
            NodeFactory nodeFactory = Objects.requireNonNull(nodeFactories.get(typeName),
                    "No node factory found for type: " + typeName);
            return nodeFactory.create(id, config);
        } catch (Exception e) {
            throw new NodeRegistryException(e);
        }
    }

    public Set<String> getRegisteredTypes(){
        return nodeFactories.keySet();
    }

    public boolean isRegistered(String typeName){
        try {
            if (typeName == null || typeName.isBlank()) {
                throw new IllegalArgumentException("typeName must be notBlank");
            }
            return nodeFactories.containsKey(typeName);
        } catch (Exception e) {
            throw new NodeRegistryException(e);
        }
    }

}
