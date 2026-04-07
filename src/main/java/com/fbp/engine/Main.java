package com.fbp.engine;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.Node;
import com.fbp.engine.node.impl.PrintNode;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        Node printNode = new PrintNode("printer-1");

        printNode.process(
                new Message(
                        Map.of("key", "value")
                )
        );
    }
}
