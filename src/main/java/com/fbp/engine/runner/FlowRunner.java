package com.fbp.engine.runner;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.State;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class FlowRunner {
    private final Flow flow;
    private ExecutorService executorService;
    private volatile State state = State.INITIALIZED;

    public FlowRunner(Flow flow) {
        this.flow = Objects.requireNonNull(flow,"flow must be notNull");
    }

    public synchronized void start() {
        if (state == State.RUNNING) {
            return;
        }

        List<String> errs = flow.validate();
        if(!errs.isEmpty()){
            throw new IllegalStateException(errs.toString());
        }

        flow.initialize();

        executorService = Executors.newCachedThreadPool();
        flow.getConnections().forEach(conn ->
                executorService.submit(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        conn.poll();
                    }
                })
        );
        state = State.RUNNING;
    }

    public synchronized void stop() {
        if (state != State.RUNNING){
            return;
        }

        flow.shutdown();

        if (executorService != null) {
            executorService.shutdownNow();
        }
        state = State.STOPPED;
    }
    public String getFlowId(){return flow.getId();}
    public State getState() { return state; }
}
