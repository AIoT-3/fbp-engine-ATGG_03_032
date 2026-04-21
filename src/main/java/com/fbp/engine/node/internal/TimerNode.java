package com.fbp.engine.node.internal;

import com.fbp.engine.message.Message;
import com.fbp.engine.core.node.AbstractNode;
import lombok.Getter;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimerNode extends AbstractNode {
    private final long intervalMs;
    @Getter
    private int tickCount;
    private ScheduledExecutorService scheduler;

    public TimerNode(String id, long intervalMs) {
        super(id);
        if (intervalMs <= 0) {
            throw new IllegalStateException("intervalMs must be more than 0");
        }

        this.tickCount = 0;
        this.intervalMs = intervalMs;
        scheduler = Executors.newSingleThreadScheduledExecutor();

        addOutputPort("out");
    }

    @Override
    public void initialize() {
        super.initialize();

        this.scheduler.scheduleAtFixedRate(()->{
            send("out", new Message(
                    Map.of("tick", tickCount, "timestamp", System.currentTimeMillis())
            ));
            tickCount++;
        },intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        if(scheduler !=null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    @Override
    public void onProcess(String portName, Message message) {
        //nothing to do
        throw new IllegalStateException();
    }
}