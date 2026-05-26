package com.fbp.engine.core.port.impl;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.core.port.OutputPort;
import com.fbp.engine.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DefaultOutputPort implements OutputPort {
    private final String name;
    private final List<Connection> connections;

    public DefaultOutputPort(String name) {
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("name must be notBlank");
        }
        this.name = name;
        this.connections = new ArrayList<>();
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void connect(Connection connection) {
        Objects.requireNonNull(connection, "connection must be notNull");

        this.connections.add(connection);
    }

    @Override
    public void send(Message message) {
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }

        for(Connection connection: connections){
            connection.deliver(message);
        }
    }
}
