package com.fbp.engine.node.internal;

import com.fbp.engine.message.Message;
import com.fbp.engine.core.node.AbstractNode;

public class PrintNode extends AbstractNode {

    public PrintNode(String id) {
        super(id);
        addInputPort("in");
    }

    @Override
    public void onProcess(String portName, Message message) {
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }
        System.out.printf("[%s] %s\n", getId(), message);
    }

}
