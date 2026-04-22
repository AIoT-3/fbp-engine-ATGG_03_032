package com.fbp.engine;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.node.external.MqttPublisherNode;
import com.fbp.engine.node.external.MqttSubscriberNode;
import com.fbp.engine.node.internal.GeneratorNode;
import com.fbp.engine.node.internal.PrintNode;

import java.util.Map;
import java.util.Random;
import java.util.random.RandomGenerator;

public class Main_12_6 {
    public static void main(String[] args) throws InterruptedException {
        FlowEngine flowEngine = new FlowEngine();
        Flow flow = new Flow("flow");

        MqttSubscriberNode subscriberNode = new MqttSubscriberNode("subscriber",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "client-sub",
                        "topic", "sensor/temp",
                        "qos", 1));
        PrintNode printNode = new PrintNode("printer");

        GeneratorNode generatorNode = new GeneratorNode("generator");
        MqttPublisherNode publisherNode = new MqttPublisherNode("publisher",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "client-pub",
                        "topic", "sensor/temp",
                        "qos", 1,
                        "retained", false));

        flow.addNode(subscriberNode)
                .addNode(printNode)
                .addNode(generatorNode)
                .addNode(publisherNode)
                .connect(subscriberNode.getId(),"out", printNode.getId(), "in")
                .connect(generatorNode.getId(),"out", publisherNode.getId(),"in");

        flowEngine.register(flow);
        flowEngine.startFlow("flow");

        Thread thread = new Thread(()->{
            while(true) {
                generatorNode.generate("temperature", Math.random() * 20 + 15);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        Thread.sleep(20000);

        flowEngine.shutdown();
    }
}
