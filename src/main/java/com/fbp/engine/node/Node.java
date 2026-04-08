package com.fbp.engine.node;

import com.fbp.engine.message.Message;

public interface Node {
    void initialize();
    void shutdown();

    String getId();
    void process(Message message);
}
