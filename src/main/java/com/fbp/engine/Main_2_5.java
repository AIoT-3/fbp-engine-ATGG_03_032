package com.fbp.engine;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.PrintNode;

import java.util.Map;

public class Main_2_5 {
    public static void main(String[] args) {
        PrintNode printNode = new PrintNode("pri-1");

        Message message = new Message(
                Map.of("key", "value")
        );

        printNode.process(message);
    }
}
