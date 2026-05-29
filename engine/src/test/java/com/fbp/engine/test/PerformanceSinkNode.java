package com.fbp.engine.test;

import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.message.Message;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class PerformanceSinkNode extends AbstractNode {
    private final CountDownLatch latch;
    private final List<Long> latencies;

    public PerformanceSinkNode(String id, CountDownLatch latch, List<Long> latencies) {
        super(id);
        this.latch = latch;
        this.latencies = latencies;
        addInputPort("in");
    }

    @Override
    public void onProcess(String portName, Message message) {
        if (message.hasKey("entryTime")) {
            long entryTime = (long) message.get("entryTime");
            long exitTime = System.nanoTime();
            latencies.add(TimeUnit.NANOSECONDS.toMillis(exitTime - entryTime));
        }
        latch.countDown();
    }
}
