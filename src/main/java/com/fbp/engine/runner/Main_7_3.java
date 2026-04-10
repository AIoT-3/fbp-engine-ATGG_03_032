package com.fbp.engine.runner;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.PrintNode;
import com.fbp.engine.node.impl.SplitNode;
import com.fbp.engine.node.impl.TimerNode;

import java.util.ArrayList;
import java.util.List;

public class Main_7_3 {
    public static void main(String[] args) {
        Flow flow = new Flow("flow");

        TimerNode timerNode = new TimerNode("timer", 300);
        SplitNode splitNode = new SplitNode("splitter", "tick", 3);
        PrintNode matchedPrinter = new PrintNode("matched-printer");
        PrintNode mismatchedPrinter = new PrintNode("mismatched-printer");

        flow.addNode(timerNode)
                .addNode(splitNode)
                .addNode(matchedPrinter)
                .addNode(mismatchedPrinter)
                .connect(timerNode.getId(), "out", splitNode.getId(), "in")
                .connect(splitNode.getId(),"match", matchedPrinter.getId(), "in")
                .connect(splitNode.getId(), "mismatch", matchedPrinter.getId(), "in");

        flow.validate();

        List<Thread> threads = new ArrayList<>();
        flow.getConnections().forEach(connection -> {
            Thread thread = new Thread(() -> {
                while(!Thread.currentThread().isInterrupted()){
                    connection.poll();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
            thread.setName(connection.getId());
            threads.add(thread);
        });

        flow.initialize();
        threads.forEach(Thread::start);

        try {Thread.sleep(5000);} catch (InterruptedException e) {}

        flow.shutdown();
        threads.forEach(Thread::interrupt);

        threads.forEach(thread -> {
            try {thread.join();} catch (InterruptedException e) {}});
    }
}
