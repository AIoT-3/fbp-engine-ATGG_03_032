package com.fbp.engine.node.internal;

import com.fbp.engine.message.Message;
import com.fbp.engine.core.node.AbstractNode;

import java.util.function.Function;

public class TransformNode extends AbstractNode {
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
    public void onProcess(String portName, Message message) {
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }

        Message result = transformer.apply(message);

        if(result != null){
            send("out", result);
        }
    }
}
