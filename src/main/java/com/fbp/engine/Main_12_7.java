package com.fbp.engine;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.node.external.MqttPublisherNode;
import com.fbp.engine.node.external.MqttSubscriberNode;
import com.fbp.engine.node.internal.FilterNode;

import java.util.Map;

public class Main_12_7 {
    public static void main(String[] args) throws InterruptedException {
        FlowEngine flowEngine = new FlowEngine();
        Flow flow = new Flow("flow");

        MqttSubscriberNode subscriberNode = new MqttSubscriberNode("subscriber",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "client-sub",
                        "topic", "sensor/temp",
                        "qos", 1));
        FilterNode filterNode = new FilterNode("filter", "temperature", 30);
        MqttPublisherNode publisherNode = new MqttPublisherNode("publisher",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "client-pub",
                        "topic", "sensor/alert",
                        "qos", 1,
                        "retained", false));

        flow.addNode(subscriberNode)
                .addNode(filterNode)
                .addNode(publisherNode)
                .connect(subscriberNode.getId(), "out", filterNode.getId(), "in")
                .connect(filterNode.getId(), "out", publisherNode.getId(), "in");

        flowEngine.register(flow);
        flowEngine.startFlow("flow");

    }
}
