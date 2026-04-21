package com.fbp.engine.node.internal;

import com.fbp.engine.message.Message;
import com.fbp.engine.core.node.AbstractNode;

import java.util.Map;

public class HumiditySensorNode extends AbstractNode {
    private double min;
    private double max;

    public HumiditySensorNode(String id, double min, double max) {
        super(id);
        this.min = min;
        this.max = max;

        addInputPort("trigger");
        addOutputPort("out");
    }

    @Override
    public void onProcess(String portName, Message message) {
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }

        double randomHumidity = Math.round((min + Math.random()*(max-min))*10)/10.0;

        Message newMessage = new Message(Map.of(
                "sensorId", getId(),
                "humidity", randomHumidity,
                "unit", "%",
                "timestamp", System.currentTimeMillis()
        ));

        send("out", newMessage);
    }
}
