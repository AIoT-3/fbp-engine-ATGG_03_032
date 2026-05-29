package com.fbp.engine.test;

import com.fbp.engine.core.engine.FlowManager;
import com.fbp.engine.core.engine.ThreadPoolConfig;
import com.fbp.engine.core.engine.runner.FlowRunner;
import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.core.registry.NodeRegistry;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

@Tag("performance")
class PerformanceTest {

    private FlowManager flowManager;
    private NodeRegistry nodeRegistry;

    static class PassThroughNode extends AbstractNode {
        public PassThroughNode(String id) {
            super(id);
            addInputPort("in");
            addOutputPort("out");
        }
        @Override
        public void onProcess(String portName, Message message) {
            send("out", message);
        }
    }

    @BeforeEach
    void setUp() {
        flowManager = FlowManager.getInstance();
        flowManager.reset();
        nodeRegistry = flowManager.getNodeRegistry();
        nodeRegistry.register("PassThrough", (id, config) -> new PassThroughNode(id));
    }

    @AfterEach
    void tearDown() {
        flowManager.reset();
    }

    @Test
    @DisplayName("처리량 및 지연 시간 기준 검증 (10,000건)")
    void testThroughputAndLatency() throws InterruptedException {
        int warmupMessages = 10000;
        int measuredMessages = 5000;
        int totalMessages = warmupMessages + measuredMessages;
        
        CountDownLatch latch = new CountDownLatch(totalMessages);
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        AtomicLong errors = new AtomicLong();

        PassThroughNode entryNode = new PassThroughNode("entry");
        PassThroughNode node1 = new PassThroughNode("node1");
        PerformanceSinkNode sinkNode = new PerformanceSinkNode("sink", latch, latencies);

        Flow flow = new Flow("perf-flow")
                .addNode(entryNode).addNode(node1).addNode(sinkNode)
                .connect("entry", "out", "node1", "in", 5000, "block")
                .connect("node1", "out", "sink", "in", 5000, "block");

        ThreadPoolConfig config = new ThreadPoolConfig();
        config.setCorePoolSize(Runtime.getRuntime().availableProcessors());
        FlowRunner runner = new FlowRunner(flow, config);
        runner.start();

        PerformanceResult result = LoadTester.runLoadTest(entryNode, "in", totalMessages, latch, latencies, errors);

        List<Long> actualLatencies = new ArrayList<>();
        if (latencies.size() > warmupMessages) {
            actualLatencies.addAll(latencies.subList(warmupMessages, latencies.size()));
        } else {
            actualLatencies.addAll(latencies);
        }
        
        double avgLatency = actualLatencies.stream().mapToLong(Long::longValue).average().orElse(0.0);
        Collections.sort(actualLatencies);
        double p99Latency = actualLatencies.isEmpty() ? 0 : actualLatencies.get((int) (actualLatencies.size() * 0.99));

        System.out.println("Throughput: " + result.getThroughput() + " msg/sec");
        System.out.println("Avg Latency (Warm): " + avgLatency + " ms");
        System.out.println("P99 Latency (Warm): " + p99Latency + " ms");

        assertTrue(result.getThroughput() >= 1000, "Throughput should be >= 1000 msgs/sec");
        assertTrue(avgLatency < 10.0, "Avg latency should be < 10ms");
        assertTrue(p99Latency < 50.0, "P99 latency should be < 50ms");
        assertTrue(result.getErrorRate() < 0.001, "Error rate should be < 0.1%");

        runner.stop();
    }

    @Test
    @DisplayName("장시간 실행 및 메모리 안정성 검증")
    void testMemoryStability() throws InterruptedException {
        int durationSecs = 5; 
        long endTime = System.currentTimeMillis() + (durationSecs * 1000);
        
        PassThroughNode entryNode = new PassThroughNode("entry");
        PerformanceSinkNode sinkNode = new PerformanceSinkNode("sink", new CountDownLatch(0), new ArrayList<>()) {
            @Override
            public void onProcess(String portName, Message message) {
            }
        };

        Flow flow = new Flow("mem-flow")
                .addNode(entryNode).addNode(sinkNode)
                .connect("entry", "out", "sink", "in", 100, "drop-newest");

        FlowRunner runner = new FlowRunner(flow);
        runner.start();

        MemoryMonitor monitor = new MemoryMonitor();
        monitor.start(1000);

        long msgCount = 0;
        while (System.currentTimeMillis() < endTime) {
            entryNode.process("in", new Message(Map.of("data", "test")));
            msgCount++;
            if (msgCount % 1000 == 0) {
                Thread.sleep(5);
            }
        }

        monitor.stop();
        runner.stop();

        assertFalse(monitor.isMonotonicallyIncreasing(), "Memory should not monotonically increase (leak)");
    }
}
