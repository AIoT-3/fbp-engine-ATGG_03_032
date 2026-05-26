package com.fbp.engine.core.registry;

import com.fbp.engine.core.node.Node;

import java.util.Map;

@FunctionalInterface
public interface NodeFactory {
    Node create(String id, Map<String, Object> config);
}