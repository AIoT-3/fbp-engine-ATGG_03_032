package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;

import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TimerNode extends AbstractNode {
    private long intervalMs;
    private int tickCount;
    private ScheduledExecutorService scheduler;

    public TimerNode(String id, long intervalMs) {
        super(id);
        if (intervalMs <= 0) {
            throw new IllegalStateException("intervalMs must be more than 0");
        }

        this.tickCount = 0;
        this.intervalMs = intervalMs;

        addOutputPort("out");
    }

    @Override
    public void initialize() {
        this.scheduler = new ScheduledThreadPoolExecutor(1);

        this.scheduler.scheduleAtFixedRate(()->{
            send("out", new Message(
                    Map.of("tick", tickCount, "timestamp", System.currentTimeMillis())
            ));
            tickCount++;
        },intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void shutdown() {
        scheduler.shutdown();
    }

    @Override
    public void onProcess(Message message) {
        //nothing to do
        throw new IllegalStateException();
    }
}