package com.fbp.engine.node;

import com.fbp.engine.message.Message;

public interface Node {
    String getId();
    void process(Message message);
}
