package com.fbp.engine.runner;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.FilterNode;
import com.fbp.engine.node.impl.LogNode;
import com.fbp.engine.node.impl.PrintNode;
import com.fbp.engine.node.impl.TimerNode;

public class Main_6_5 {
    public static void main(String[] args) {
        TimerNode timerNode = new TimerNode("timer", 500);
        LogNode logNode = new LogNode("Logger");
        FilterNode filterNode = new FilterNode("filter", "tick", 3);
        PrintNode printNode = new PrintNode("printer");

        Connection ttl = new Connection("ttl");
        timerNode.getOutputPort("out").connect(ttl);

        Connection ltf = new Connection("ltf");
        logNode.getOutputPort("out").connect(ltf);

        Connection ftp = new Connection("ftp");
        filterNode.getOutputPort("out").connect(ftp);

        Thread logger = new Thread(
                () -> {
                    while(true) {
                        Message message = ttl.poll();
                        if (message != null) {
                            logNode.process(message);
                        }

                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
        );logger.setName("logger");

        Thread filter = new Thread(
                () -> {
                    while (true){
                        Message message = ltf.poll();
                        if(message != null){
                            filterNode.process(message);
                        }

                        try{
                            Thread.sleep(700);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
        );filter.setName("filter");

        Thread printer = new Thread(
                () -> {
                    while(true) {
                        Message message = ftp.poll();
                        if (message != null) {
                            printNode.process(message);
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
        );printer.setName("printer");

        timerNode.initialize();
        logger.start();
        filter.start();
        printer.start();

        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {}

        timerNode.shutdown();
        logger.interrupt();
        filter.interrupt();
        printer.interrupt();

        try {logger.join();} catch (InterruptedException e) {}

        try {filter.join();} catch (InterruptedException e) {}

        try {printer.join();} catch (InterruptedException e) {}
    }
}
