package com.fbp.engine.runner;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.FilterNode;
import com.fbp.engine.node.impl.GeneratorNode;
import com.fbp.engine.node.impl.PrintNode;

import java.util.random.RandomGenerator;

public class Main_4_56 {
    static volatile boolean gen_running = true;
    static volatile boolean filter_running = true;

    public static void main(String[] args) {
        GeneratorNode generatorNode = new GeneratorNode("gen-1");
        FilterNode filterNode = new FilterNode("filter-temp", "temperature", 35);
        PrintNode printNode = new PrintNode("pri-1");

        Connection connection = new Connection("gen-to-fil");
        generatorNode.getOutputPort().connect(connection);
        connection.setTarget(filterNode.getInputPort());

        Connection connection1 = new Connection("fil-to-pri");
        filterNode.getOutputPort().connect(connection1);
        connection1.setTarget(printNode.getInputPort());

        Thread gen = new Thread(
                () -> {
                    for(int i=0; i<100; i++){
                        generatorNode.generate("temperature", 27+ RandomGenerator.getDefault().nextInt(10));
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }gen_running = false;
                }
        );gen.setName("generator");

        Thread filter = new Thread(
                () -> {
                    while (gen_running || connection.getBufferSize() !=0){
                        System.out.println("gen-to-fil connection... current buffesr size: " + connection.getBufferSize());
                        Message message = connection.poll();
                        if(message != null){
                            filterNode.process(message);
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    filter_running = false;
                }
        ); filter.setName("filter");

        Thread printer = new Thread(
                () -> {
                    while (filter_running || connection1.getBufferSize() != 0){
                        System.out.println("fil-to-pri connection... current buffesr size: " + connection1.getBufferSize());
                        Message message = connection1.poll();
                        if(message != null){
                            printNode.process(message);
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
        );printer.setName("printer");

        gen.start();
        filter.start();
        printer.start();


    }
}
