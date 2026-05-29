package com.fbp.engine.node.flow;

import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.message.Message;

import java.util.function.BiConsumer;

public class SubFlowOutputNode extends AbstractNode {

    private final String publicPortName;
    private final BiConsumer<String, Message> parentSender;

    public SubFlowOutputNode(String id, String publicPortName, BiConsumer<String, Message> parentSender) {
        super(id);
        this.publicPortName = publicPortName;
        this.parentSender = parentSender;
        addInputPort("in");
    }

    @Override
    public void onProcess(String portName, Message message) {
        parentSender.accept(publicPortName, message);
    }
}
