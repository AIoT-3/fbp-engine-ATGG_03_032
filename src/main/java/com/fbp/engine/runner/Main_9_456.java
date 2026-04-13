package com.fbp.engine.runner;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.node.impl.*;

public class Main_9_456 {
    public static void main(String[] args) {
        FlowEngine flowEngine = new FlowEngine();

        Flow flow = new Flow("flow");
        flowEngine.register(flow);

        TimerNode timerNode = new TimerNode("timer", 1000);

        TemperatureSensorNode temperatureSensorNode = new TemperatureSensorNode("sensor-temperature", 15.0, 45.0);
        ThresholdFilterNode temperatureThresholdFilterNode = new ThresholdFilterNode("threshold-temperature-filter", "temperature", 30);

        HumiditySensorNode humiditySensorNode = new HumiditySensorNode("sensor-humidity", 40.0, 90.0);
        ThresholdFilterNode humidityThresholdFilterNode =  new ThresholdFilterNode("threshold-humidity-filter", "humidity", 70);

        AlertNode alertNode = new AlertNode("alerter");
        LogNode logNode = new LogNode("logger");
        FileWriterNode fileWriterNode = new FileWriterNode("fileWriter", "submit/output.txt");

        flow.addNode(timerNode)
                .addNode(temperatureSensorNode)
                .addNode(temperatureThresholdFilterNode)
                .addNode(humiditySensorNode)
                .addNode(humidityThresholdFilterNode)
                .addNode(alertNode)
                .addNode(logNode)
                .addNode(fileWriterNode)
                .connect(timerNode.getId(), "out", temperatureSensorNode.getId(), "trigger")
                .connect(timerNode.getId(), "out", humiditySensorNode.getId(), "trigger")
                .connect(temperatureSensorNode.getId(), "out", temperatureThresholdFilterNode.getId(), "in")
                .connect(humiditySensorNode.getId(), "out", humidityThresholdFilterNode.getId(), "in")
                .connect(temperatureThresholdFilterNode.getId(),"alert", alertNode.getId(),"in")
                .connect(temperatureThresholdFilterNode.getId(),"normal", logNode.getId(),"in")
                .connect(temperatureThresholdFilterNode.getId(), "normal", fileWriterNode.getId(), "in")
                .connect(humidityThresholdFilterNode.getId(), "alert", alertNode.getId(), "in")
                .connect(humidityThresholdFilterNode.getId(), "normal", logNode.getId(), "in");

        flowEngine.startFlow("flow");
        try {Thread.sleep(10000);} catch (InterruptedException e) {}
        flowEngine.stopFlow("flow");
    }
}
