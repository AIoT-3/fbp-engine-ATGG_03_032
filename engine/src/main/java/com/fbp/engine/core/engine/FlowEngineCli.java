package com.fbp.engine.core.engine;

import java.util.Objects;
import java.util.Scanner;

public class FlowEngineCli {
    private FlowEngine flowEngine;
    private Scanner scanner;

    public FlowEngineCli(FlowEngine flowEngine) {
        this.flowEngine = Objects.requireNonNull(flowEngine, "flowEngine must be notNull");
        this.scanner = new Scanner(System.in);
    }

    public void run(){
        while(true) {
            System.out.print("fbp> ");
            String next = scanner.nextLine();
            String[] commandAndValue = next.split("\\s+");
            if (commandAndValue.length == 1) {
                if (commandAndValue[0].equals("list")) {
                    flowEngine.listFlows();
                } else if (commandAndValue[0].equals("exit")) {
                    flowEngine.shutdown();
                    break;
                }
            } else if (commandAndValue.length == 2) {
                if (commandAndValue[0].equals("start")) {
                    flowEngine.startFlow(commandAndValue[1].replace("\n", "").trim());
                } else if (commandAndValue[0].equals("stop")) {
                    flowEngine.stopFlow(commandAndValue[1]);
                }
            }
        }
    }
}
