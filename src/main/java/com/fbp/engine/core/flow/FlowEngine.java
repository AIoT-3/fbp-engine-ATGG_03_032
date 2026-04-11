package com.fbp.engine.core.flow;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@Slf4j
public class FlowEngine {
    protected enum State{
        INITIALIZED, RUNNING, STOPPED
    }
    private Map<String, Flow> flows;
    private State state;

    public FlowEngine() {
        this.flows = new HashMap<>();
        this.state = State.INITIALIZED;
    }

    public void cliRun(){
        FlowEngineCli flowEngineCli = new FlowEngineCli(this);
        flowEngineCli.run();
    }

    public void register(Flow flow){
        if(flow == null){
            throw new IllegalArgumentException("flow must be notNull");
        }

        flows.put(flow.getId(), flow);
        log.info(String.format("[Engine] flow '%s' registered", flow.getId()));
    }

    public void startFlow(String flowId){
        Flow flow = flows.get(flowId);
        if(flow == null){
            throw new IllegalArgumentException("not founded flow flowId:" +flowId);
        }
        List<String> errs = flow.validate();
        if(!errs.isEmpty()){
            throw new IllegalStateException(errs.toString());
        }

        flow.initialize();
        this.state=State.RUNNING;

        log.info(String.format("[Engine] flow '%s' started", flowId));
    }

    public void stopFlow(String flowId){
        Flow flow = flows.get(flowId);
        if(flow == null){
            throw new IllegalArgumentException("not founded flow flowId:" +flowId);
        }
        flow.shutdown();

        log.info(String.format("[Engine] flow '%s' stopped", flowId));

        State state = State.STOPPED;
        for(Flow floww: flows.values()){
            if(floww.getState()==State.RUNNING){
                state=State.RUNNING;
            }
        }
        this.state = state;
    }

    public void shutdown(){
        flows.keySet().iterator().forEachRemaining(this::stopFlow);
        this.state = State.STOPPED;
    }

    public State getState(){
        return this.state;
    }

    public Map<String, Flow> getFlows(){
        return this.flows;
    }

    public void listFlows(){
        int i=0;
        for (Flow value : flows.values()) {
            i++;
            System.out.printf("[%d] [%s] state:%s\n", i,value.getId(), value.getState());
        }

    }

}
