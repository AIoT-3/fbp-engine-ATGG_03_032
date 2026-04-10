package com.fbp.engine.runner;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.node.impl.LogNode;

public class Main_7_circularRef {
    public static void main(String[] args) {
        LogNode logNode = new LogNode("l1");
        LogNode logNode1 = new LogNode("l2");
        LogNode logNode2 = new LogNode("l3");
        LogNode logNode3 = new LogNode("l4");
        LogNode logNode4 = new LogNode("l5");
        LogNode logNode5 = new LogNode("l6");

        Flow flow = new Flow("flow");

        flow.addNode(logNode)
                .addNode(logNode1)
                .addNode(logNode2)
                .addNode(logNode3)
                .addNode(logNode4)
                .addNode(logNode5)
                .connect("l1","out","l2","in")
                .connect("l2", "out", "l3", "in")
                .connect("l3", "out", "l4", "in")
                .connect("l4", "out", "l5", "in")
                .connect("l5", "out", "l6", "in")
                .connect("l5", "out", "l1", "in")
                        .connect("l6","out", "l3","in")
                                .connect("l6", "out", "l6","in");

        System.out.println(flow.validate());
    }
}
