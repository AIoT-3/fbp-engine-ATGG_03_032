package com.fbp.engine.test;

import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.message.Message;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class LoadTester {

    public static PerformanceResult runLoadTest(
            AbstractNode entryNode, 
            String inputPort, 
            int totalMessages, 
            CountDownLatch latch, 
            List<Long> latencies,
            AtomicLong errors) throws InterruptedException {

        ExecutorService producerPool = Executors.newSingleThreadExecutor();
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < totalMessages; i++) {
            final int msgId = i;
            producerPool.submit(() -> {
                try {
                    Message msg = new Message(Map.of(
                            "id", msgId, 
                            "payload", "load-test-data",
                            "entryTime", System.nanoTime()
                    ));
                    entryNode.process(inputPort, msg);
                } catch (Exception e) {
                    errors.incrementAndGet();
                    latch.countDown(); 
                }
            });
        }

        producerPool.shutdown();
        latch.await(60, TimeUnit.SECONDS);
        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;

        double throughput = (totalMessages / (double) durationMs) * 1000;
        
        double avgLatency = latencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        Collections.sort(latencies);
        double p99Latency = 0.0;
        if (!latencies.isEmpty()) {
            int p99Index = (int) (latencies.size() * 0.99);
            if (p99Index >= latencies.size()) p99Index = latencies.size() - 1;
            p99Latency = latencies.get(p99Index);
        }

        return PerformanceResult.builder()
                .totalMessages(totalMessages)
                .durationMs(durationMs)
                .throughput(throughput)
                .avgLatencyMs(avgLatency)
                .p99LatencyMs(p99Latency)
                .errors(errors.get())
                .errorRate((double) errors.get() / totalMessages)
                .build();
    }
}
