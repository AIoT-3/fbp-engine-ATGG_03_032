package com.fbp.engine.node.flow;

import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.message.Message;

public class SubFlowInputNode extends AbstractNode {

    public SubFlowInputNode(String id) {
        super(id);
        addOutputPort("out");
    }

    @Override
    public void onProcess(String portName, Message message) {
        send("out", message);
    }
}
