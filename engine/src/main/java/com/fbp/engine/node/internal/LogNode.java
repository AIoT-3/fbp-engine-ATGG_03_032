package com.fbp.engine.node.internal;

import com.fbp.engine.message.Message;
import com.fbp.engine.core.node.AbstractNode;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LogNode extends AbstractNode {
    public LogNode(String id) {
        super(id);

        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    public void onProcess(String portName, Message message) {
        String time = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(LocalTime.now());
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }

        System.out.printf("[%s][%s] %s\n",
                getId(),
                time,
                message);
        send("out", message);
    }
}
