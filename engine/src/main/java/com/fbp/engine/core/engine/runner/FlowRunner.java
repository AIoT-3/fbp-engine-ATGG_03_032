package com.fbp.engine.core.engine.runner;

import com.fbp.engine.core.engine.ThreadPoolConfig;
import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.engine.State;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
public class FlowRunner {
    private final Flow flow;
    private final ThreadPoolConfig threadPoolConfig;
    private ThreadPoolExecutor executorService;
    private volatile State state = State.INITIALIZED;

    public FlowRunner(Flow flow) {
        this(flow, new ThreadPoolConfig());
    }

    public FlowRunner(Flow flow, ThreadPoolConfig threadPoolConfig) {
        this.flow = Objects.requireNonNull(flow, "flow must be notNull");
        this.threadPoolConfig = Objects.requireNonNull(threadPoolConfig, "threadPoolConfig must be notNull");
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

        executorService = new ThreadPoolExecutor(
                threadPoolConfig.getCorePoolSize(),
                threadPoolConfig.getMaxPoolSize(),
                threadPoolConfig.getKeepAliveTimeMs(),
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(threadPoolConfig.getQueueCapacity()),
                (r, executor) -> log.warn("Task rejected from flow runner thread pool: {}", r.toString())
        );

        flow.getConnections().forEach(conn ->
                executorService.submit(() -> {
                    Thread.currentThread().setName(conn.getId());
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
    public ThreadPoolExecutor getExecutorService() { return executorService; }
}
