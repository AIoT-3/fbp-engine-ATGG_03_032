package com.fbp.engine.runner;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.GeneratorNode;
import com.fbp.engine.node.impl.PrintNode;

import java.util.random.RandomGenerator;

public class Main_4_4 {
    static volatile boolean running = true;

    public static void main(String[] args) {
        GeneratorNode generatorNode = new GeneratorNode("gen-1");

        PrintNode printNode = new PrintNode("pri-1");

        Connection connection = new Connection("gen-to-pri");
        generatorNode.getOutputPort().connect(connection);
        connection.setTarget(printNode.getInputPort());

        Thread provider = new Thread(
                () -> {
                    for (int i = 0; i < 5; i++){
                        generatorNode.generate("temperature", 28 + RandomGenerator.getDefault().nextInt(10));
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    running = false;
                }
        );

        Thread consumer = new Thread(
                () -> {
                    while(running || !Thread.currentThread().getState().equals(Thread.State.RUNNABLE)){
                        Message message = connection.poll();
                        if(message != null){
                            printNode.process(message);
                        }
                    }
                }
        );

        provider.start();
        consumer.start();

        try {
            consumer.join();
            provider.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
