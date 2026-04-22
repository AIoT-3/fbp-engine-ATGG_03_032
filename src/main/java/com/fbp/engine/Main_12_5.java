package com.fbp.engine;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.node.external.MqttSubscriberNode;
import com.fbp.engine.node.internal.PrintNode;
import org.eclipse.paho.mqttv5.common.MqttSubscription;

import java.util.Map;

public class Main_12_5 {
    public static void main(String[] args) throws InterruptedException {
        FlowEngine flowEngine = new FlowEngine();
        Flow flow = new Flow("MqttSubscriberNode → PrintNode");

        /*
        brokerUrl(String, 예: "tcp://localhost:1883"),
clientId(String),
topic(String),
qos(int, 기본 1)
         */
        MqttSubscriberNode subscriberNode = new MqttSubscriberNode("subscriber",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "client-1",
                        "topic", "sensor/temp",
                        "qos", 1));
        PrintNode printNode = new PrintNode("printer");

        flow.addNode(subscriberNode)
                .addNode(printNode)
                .connect(subscriberNode.getId(),"out", printNode.getId(), "in");

        flowEngine.register(flow);
        flowEngine.startFlow("MqttSubscriberNode → PrintNode");

        Thread.sleep(30000);

        flowEngine.shutdown();
    }
}