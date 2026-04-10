package com.fbp.engine.runner;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.GeneratorNode;
import com.fbp.engine.node.impl.PrintNode;
import com.fbp.engine.node.impl.TransformNode;

import java.util.Objects;
import java.util.random.RandomGenerator;

public class Main_6_2 {
    public static void main(String[] args) {
        GeneratorNode generatorNode = new GeneratorNode("generator-1");
        TransformNode transformNode = new TransformNode("transform-1", message -> {
            Double fc = message.get("temperature");
            double sc = (fc-32) / 1.8;
            return message.withEntry("temperature", sc);
        });
        PrintNode prt = new PrintNode("prt");

        Connection genToTrf = new Connection("gen-to-trf");
        generatorNode.getOutputPort("out").connect(genToTrf);

        Connection trfToPrt = new Connection("trf-to-prt");
        transformNode.getOutputPort("out").connect(trfToPrt);

        Thread generator = new Thread(
                () -> {
                    while (true) {
                        generatorNode.generate("temperature", Double.valueOf(80 + RandomGenerator.getDefault().nextInt(10)));
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
        );
        generator.setName("generator-1");

        Thread transformer = new Thread(
                () -> {
                    while(true){
                        Message message = genToTrf.poll();
                        if (Objects.nonNull(message)) {
                            transformNode.process(message);
                        }

                        try {
                            Thread.sleep(700);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
        );
        transformer.setName("transformer");

        Thread printer = new Thread(
                () -> {
                    while (true){
                        Message msg = trfToPrt.poll();
                        if (Objects.nonNull(msg)) {
                            prt.process(msg);
                        }
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException e) {
                            break;
                        }
                    }
                }
        );

        generator.start();
        transformer.start();
        printer.start();


        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        generator.interrupt();
        transformer.interrupt();
        printer.interrupt();

        try {generator.join();} catch (InterruptedException e) {}
        try {transformer.join();} catch (InterruptedException e) {}
        try {printer.join();} catch (InterruptedException e) {}
    }
}
