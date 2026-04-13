package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;

import java.util.Map;

public class TemperatureSensorNode extends AbstractNode{
    private double min;
    private double max;

    public TemperatureSensorNode(String id, double min, double max) {
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

        double randomTemperature = Math.round((min + Math.random()*(max-min))*10)/10.0;

        Message newMessage = new Message(Map.of(
                "sensorId", getId(),
                "temperature", randomTemperature,
                "unit", "C",
                "timestamp", System.currentTimeMillis()
        ));

        send("out", newMessage);
    }
}
