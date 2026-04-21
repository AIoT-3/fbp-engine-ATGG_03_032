package com.fbp.engine;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.node.internal.*;

import java.util.UUID;

public class Main_10_5 {
    public static void main(String[] args) {
        FlowEngine flowEngine = new FlowEngine();
        Flow flow = new Flow("flow");
        flowEngine.register(flow);

        TimerNode trigger = new TimerNode("trigger", 100);
        TemperatureSensorNode temperatureSensorNode = new TemperatureSensorNode("sensor-temperature", 15, 45);
        ThresholdFilterNode thresholdFilterNode = new ThresholdFilterNode("filter-temperature", "temperature", 30);
        AlertNode alertNode = new AlertNode("alerter");
        LogNode logNode = new LogNode("logger");
        FileWriterNode fileWriterNode = new FileWriterNode("writer-file", "submit/" + UUID.randomUUID().toString() +".txt");

        flow.addNode(trigger)
                .addNode(temperatureSensorNode)
                .addNode(thresholdFilterNode)
                .addNode(alertNode)
                .addNode(logNode)
                .addNode(fileWriterNode)
                .connect(trigger.getId(),"out", temperatureSensorNode.getId(), "trigger")
                .connect(temperatureSensorNode.getId(),"out", thresholdFilterNode.getId(), "in")
                .connect(thresholdFilterNode.getId(),"alert", alertNode.getId(), "in")
                .connect(thresholdFilterNode.getId(), "normal", logNode.getId(), "in")
                .connect(logNode.getId(), "out", fileWriterNode.getId(), "in");

        flowEngine.startFlow(flow.getId());

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        flowEngine.shutdown();
    }
}
