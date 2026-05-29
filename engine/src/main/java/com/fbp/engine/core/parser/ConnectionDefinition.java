package com.fbp.engine.core.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConnectionDefinition {
    private String from;
    private String to;
    private Integer capacity;
    private String backpressure;

    public ConnectionDefinition(String from, String to) {
        this.from = from;
        this.to = to;
    }
}
