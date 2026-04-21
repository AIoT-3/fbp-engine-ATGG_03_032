package com.fbp.engine.node.internal;

import com.fbp.engine.message.Message;
import com.fbp.engine.core.node.AbstractNode;

import java.util.concurrent.atomic.AtomicLong;

public class CounterNode extends AbstractNode {
    private AtomicLong count;

    public CounterNode(String id) {
        super(id);
        count = new AtomicLong(0);
        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    public void onProcess(String portName, Message message) {
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }
        Message newMessage = message.withEntry("count", count.addAndGet(1));

        send("out", newMessage);
    }

    @Override
    public void shutdown() {
        System.out.printf("[%s] Total processed messages: %l cases", getId(), count.get());
    }
}
