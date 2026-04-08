package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;

import java.util.function.Function;

public class TransformNode extends AbstractNode{
    private final Function<Message, Message> transformer;

    public TransformNode(String id, Function<Message, Message> transformer) {
        super(id);

        if(transformer == null){
            throw new IllegalStateException("transformer must be notNull");
        }
        this.transformer = transformer;
        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    public void onProcess(Message message) {
        Message result = transformer.apply(message);

        if(result != null){
            send("out", result);
        }
    }
}
