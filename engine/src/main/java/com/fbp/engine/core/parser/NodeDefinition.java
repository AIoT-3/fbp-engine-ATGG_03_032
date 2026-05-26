package com.fbp.engine.core.parser;

import com.fbp.engine.core.node.Node;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
public class NodeDefinition {
    private String id;
    private String type;
    private Map<String, Object> config;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getConfig() {
        if(config==null){
            return new HashMap<>();
        }
        return new HashMap<>(config);
    }
}