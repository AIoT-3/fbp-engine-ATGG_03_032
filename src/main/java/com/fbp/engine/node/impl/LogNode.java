package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class LogNode extends AbstractNode {
    public LogNode(String id) {
        super(id);

        addInputPort("in");
        addOutputPort("out");
    }

    @Override
    public void onProcess(Message message) {
        System.out.printf("[%s][%s] %s",
                getId(),
                DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(LocalTime.now()),
                message);
        send("out", message);
    }
}
