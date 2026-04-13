package com.fbp.engine.core.port.impl;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;

import java.util.Objects;

public class DefaultInputPort implements InputPort {
    private final String name;
    private final Node owner;

    public DefaultInputPort(String name, Node owner) {
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("name must be notBlank");
        }
        Objects.requireNonNull(owner, "owner node must be notNull");

        this.name = name;
        this.owner = owner;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void receive(Message message) {
        if(message == null){
            return;
        }
        owner.process(this.name,message);
    }
}
