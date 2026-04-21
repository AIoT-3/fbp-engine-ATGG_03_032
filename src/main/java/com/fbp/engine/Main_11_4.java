package com.fbp.engine;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.network.server.TcpEchoServer;
import com.fbp.engine.node.external.EchoProtocolNode;
import com.fbp.engine.node.internal.TemperatureSensorNode;
import com.fbp.engine.node.internal.TimerNode;

import java.net.InetAddress;
import java.util.HashMap;

public class Main_11_4 {
    public static void main(String[] args) {
        TcpEchoServer tcpEchoServer = new TcpEchoServer();
        tcpEchoServer.start();

        Flow flow = new Flow("flow");
        TimerNode timerNode = new TimerNode("trigger", 100);
        TemperatureSensorNode temperatureSensorNode = new TemperatureSensorNode("temperature-generator", -5, 40);
        EchoProtocolNode echoProtocolNode = new EchoProtocolNode("echo", new HashMap<>(), InetAddress.getLoopbackAddress(), tcpEchoServer.getPort());

        flow.addNode(timerNode)
                .addNode(temperatureSensorNode)
                .addNode(echoProtocolNode)
                .connect(timerNode.getId(),"out", temperatureSensorNode.getId(), "trigger")
                .connect(temperatureSensorNode.getId(), "out", echoProtocolNode.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);

        flowEngine.startFlow("flow");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {}

        flowEngine.shutdown();
        tcpEchoServer.stop();
    }
}
