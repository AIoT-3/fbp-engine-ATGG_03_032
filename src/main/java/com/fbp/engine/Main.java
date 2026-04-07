package com.fbp.engine;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;
import com.fbp.engine.node.impl.FilterNode;
import com.fbp.engine.node.impl.GeneratorNode;
import com.fbp.engine.node.impl.PrintNode;

import java.util.Map;
import java.util.random.RandomGenerator;

public class Main {
    public static void main(String[] args) {
        System.out.println("-".repeat(30) + " gen -> single-connection " + "-".repeat(30));
        GeneratorNode gen = new GeneratorNode("gen-1");

        Connection connection = new Connection("conn-1");
        gen.getOutputPort().connect(connection);

        PrintNode printNode = new PrintNode("printer-1");
        connection.setTarget(printNode.getInputPort());

        for(int i=0; i<5; i++){
            gen.generate("temperature", 20 + RandomGenerator.getDefault().nextInt(10));
        }
        System.out.println("-".repeat(86));

        System.out.println();

        System.out.println("-".repeat(30) + " gen -> Multi-connection " + "-".repeat(30));
        Connection connection1 = new Connection("conn-2");
        gen.getOutputPort().connect(connection1);

        PrintNode printNode1 = new PrintNode("printer-2");
        connection1.setTarget(printNode1.getInputPort());

        for(int i=0; i<5; i++){
            gen.generate("temperature", 30 + RandomGenerator.getDefault().nextInt(10));
        }
        System.out.println("-".repeat(86));

        System.out.println();

        System.out.println("-".repeat(10) + " gen -> single-connection -> filter -> multi-connection -> printer " + "-".repeat(19));
        GeneratorNode generatorNode = new GeneratorNode("generator");

        FilterNode filterNode = new FilterNode("temperature-filter", "temperature", 35);

        Connection genToFil = new Connection("genToFil");
        generatorNode.getOutputPort().connect(genToFil);
        genToFil.setTarget(filterNode.getInputPort());

        PrintNode printer1 = new PrintNode("prt-1");
        PrintNode printer2 = new PrintNode("ptr-2");

        Connection filToPrt1 = new Connection("filToPrt1");
        filterNode.getOutputPort().connect(filToPrt1);
        filToPrt1.setTarget(printer1.getInputPort());

        Connection fillToPrt2 = new Connection("filToPrt2");
        filterNode.getOutputPort().connect(fillToPrt2);
        fillToPrt2.setTarget(printer2.getInputPort());

        for(int i=0; i<10; i++){
            generatorNode.generate("temperature", 30 + RandomGenerator.getDefault().nextInt(10));
        }
        System.out.println("-".repeat(90));


    }
}
