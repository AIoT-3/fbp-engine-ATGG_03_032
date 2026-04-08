package com.fbp.engine.runner;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.node.impl.GeneratorNode;
import com.fbp.engine.node.impl.PrintNode;
import com.fbp.engine.node.impl.TransformNode;

import java.util.random.RandomGenerator;

public class Main_6_2 {
    public static void main(String[] args) {
        GeneratorNode gen = new GeneratorNode("gen");
        TransformNode trf = new TransformNode("trf", message -> {
            Object rawValue = message.get("temperature");
            double fc = 0;
            if(rawValue instanceof Number){
                fc = (((Number) rawValue).doubleValue());
            }
            double sc = (fc-32) / 1.8;
            return message.withEntry("temperature", sc);
        });
        PrintNode prt = new PrintNode("prt");

        Connection genToTrf = new Connection("gen-to-trf");
        gen.getOutputPort().connect(genToTrf);
        genToTrf.setTarget(trf.getInputPort("in"));

        Connection trfToPrt = new Connection("trf-to-prt");
        trf.getOutputPort("out").connect(trfToPrt);
        trfToPrt.setTarget(prt.getInputPort("in"));

        Thread generator = new Thread(
                () -> {
                    while (true) {
                        gen.generate("temperature", 80 + RandomGenerator.getDefault().nextInt(10));
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
        );
        generator.setName("generator-1");

        Thread transformer = new Thread(
                () -> {
                    while(true){
                        genToTrf.poll();
                        try {
                            Thread.sleep(700);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
        );
        transformer.setName("transformer");

        Thread printer = new Thread(
                () -> {
                    while (true){
                        trfToPrt.poll();
                        try {
                            Thread.sleep(800);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
        );

        generator.start();
        transformer.start();
        printer.start();

        try {
            generator.join();
            transformer.join();
            printer.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
