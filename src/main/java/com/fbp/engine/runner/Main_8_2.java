package com.fbp.engine.runner;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.node.impl.FilterNode;
import com.fbp.engine.node.impl.LogNode;
import com.fbp.engine.node.impl.PrintNode;
import com.fbp.engine.node.impl.TimerNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main_8_2 {
    public static void main(String[] args) {
        FlowEngine flowEngine = new FlowEngine();

        Flow flow = new Flow("flow");
        flowEngine.register(flow);

        TimerNode timerNode = new TimerNode("timer", 500);
        LogNode logNode = new LogNode("Logger");
        FilterNode filterNode = new FilterNode("filter", "tick", 3);
        PrintNode printNode = new PrintNode("printer");

        flow.addNode(timerNode)
                .addNode(logNode)
                .addNode(filterNode)
                .addNode(printNode)
                .connect(timerNode.getId(),"out", logNode.getId(), "in")
                .connect(logNode.getId(), "out", filterNode.getId(), "in")
                .connect(filterNode.getId(), "out", printNode.getId(), "in");

        flowEngine.cliRun();
    }
}
