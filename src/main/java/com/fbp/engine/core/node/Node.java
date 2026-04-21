package com.fbp.engine.core.node;

import com.fbp.engine.message.Message;

public interface Node {
    void initialize();
    void shutdown();

    String getId();
    void process(String portName, Message message);
}
