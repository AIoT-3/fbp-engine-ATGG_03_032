package com.fbp.engine.core.engine;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.engine.runner.FlowRunner;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class FlowEngine {
    private final Map<String, FlowRunner> runners = new HashMap<>();

    public void cliRun(){
        FlowEngineCli flowEngineCli = new FlowEngineCli(this);
        flowEngineCli.run();
    }

    public void register(Flow flow){
        if(flow == null){
            throw new IllegalArgumentException("flow must be notNull");
        }

        FlowRunner existing = runners.get(flow.getId());
        if (existing != null) {
            throw new IllegalArgumentException(
                    String.format("flow already registered. flowId: %s", flow.getId())
            );
        }

        runners.put(flow.getId(), new FlowRunner(flow));

        log.info(String.format("[Engine] flow '%s' registered", flow.getId()));
    }

    public void startFlow(String flowId){
        FlowRunner flowRunner = runners.get(flowId);
        if(flowRunner == null){
            throw new IllegalArgumentException(String.format("not founded flow flowId:%s", flowId));
        }

        flowRunner.start();

        log.info(String.format("[Engine] flow '%s' started", flowId));
    }

    public void stopFlow(String flowId){
        FlowRunner flowRunner = runners.get(flowId);
        if(flowRunner == null){
            throw new IllegalArgumentException(String.format("not founded flow flowId:%s", flowId));
        }

        flowRunner.stop();

        log.info(String.format("[Engine] flow '%s' stopped", flowId));
    }

    public void shutdown(){
        runners.values().forEach(FlowRunner::stop);
    }

    public void listFlows(){
        int i=0;

        for (FlowRunner runner : runners.values()) {
            i++;
            System.out.printf("[%d] [%s] state:%s\n", i, runner.getFlowId(), runner.getState());
        }
    }

    public State getState() {
        if (runners.isEmpty()) {
            return State.INITIALIZED;
        }
        if (runners.values().stream().anyMatch(r -> r.getState() == State.RUNNING)) {
            return State.RUNNING;
        }
        if (runners.values().stream().anyMatch(r -> r.getState() == State.INITIALIZED)) {
            return State.INITIALIZED;
        }
        return State.STOPPED;
    }
}
