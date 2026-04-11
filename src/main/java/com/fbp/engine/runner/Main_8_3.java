package com.fbp.engine.runner;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.node.impl.PrintNode;
import com.fbp.engine.node.impl.TimerNode;

public class Main_8_3 {
    public static void main(String[] args) {
        FlowEngine flowEngine = new FlowEngine();

        Flow flowA = new Flow("flowA");
        Flow flowB = new Flow("flowB");
        flowEngine.register(flowA);
        flowEngine.register(flowB);

        TimerNode timerNodeA = new TimerNode("timer", 500);
        PrintNode printNodeA = new PrintNode("A");

        flowA.addNode(timerNodeA)
                .addNode(printNodeA)
                .connect("timer", "out", "A", "in");

        TimerNode timerNodeB = new TimerNode("timer", 1000);
        PrintNode printNodeB = new PrintNode("B");

        flowB.addNode(timerNodeB)
                .addNode(printNodeB)
                .connect("timer", "out", "B", "in");

        flowEngine.cliRun();
    }
}
