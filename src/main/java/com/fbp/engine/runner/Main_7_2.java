package com.fbp.engine.runner;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.FilterNode;
import com.fbp.engine.node.impl.LogNode;
import com.fbp.engine.node.impl.PrintNode;
import com.fbp.engine.node.impl.TimerNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class Main_7_2 {
    public static void main(String[] args) {
        Flow flow = new Flow("flow");

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
            try {thread.join();} catch (InterruptedException e) {}
        });
    }
}
