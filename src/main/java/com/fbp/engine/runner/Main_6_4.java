package com.fbp.engine.runner;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.PrintNode;
import com.fbp.engine.node.impl.SplitNode;
import com.fbp.engine.node.impl.TimerNode;

import java.util.Objects;

public class Main_6_4 {
    public static void main(String[] args) {
        TimerNode timerNode = new TimerNode("timer", 300);
        SplitNode splitNode = new SplitNode("splitter", "tick", 3);
        PrintNode matchedPrinter = new PrintNode("matched-printer");
        PrintNode mismatchedPrinter = new PrintNode("mismatched-printer");

        Connection timerToSplit = new Connection("tim-to-spl");
        timerNode.getOutputPort("out").connect(timerToSplit);

        Connection splitterToMatchedPrinter = new Connection("spl-to-mat");
        splitNode.getOutputPort("match").connect(splitterToMatchedPrinter);

        Connection splitterToMismatchedPrinter = new Connection("spl-to-mis");
        splitNode.getOutputPort("mismatch").connect(splitterToMismatchedPrinter);

        Thread splitter = new Thread(
                () -> {
                    while (true) {
                        Message message = timerToSplit.poll();
                        if(message != null)
                        splitNode.process(message);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
        );

        Thread match = new Thread(
                () -> {
                    while (true) {
                        Message message = splitterToMatchedPrinter.poll();
                        if(message != null) {
                            matchedPrinter.process(message);
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
        );

        Thread mismatch = new Thread(
                () -> {
                    while (true) {
                        Message message = splitterToMismatchedPrinter.poll();
                        if(message != null) {
                            mismatchedPrinter.process(message);
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
        );

        timerNode.initialize();
        splitter.start();
        match.start();
        mismatch.start();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {}

        timerNode.shutdown();
        splitter.interrupt();
        match.interrupt();
        mismatch.interrupt();

        try {
            splitter.join();
            match.join();
            mismatch.join();
        } catch (InterruptedException e) {}


    }
}
