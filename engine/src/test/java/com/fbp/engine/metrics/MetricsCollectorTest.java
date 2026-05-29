package com.fbp.engine.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class MetricsCollectorTest {

    private MetricsCollector collector;
    private static final String FLOW_ID = "test-flow";
    private static final String NODE_ID = "test-node";

    @BeforeEach
    void setUp() {
        collector = MetricsCollector.getInstance();
        collector.resetAll();
    }

    @Test
    @DisplayName("처리 건수 기록: recordProcessing 호출 후 처리 건수 증가")
    void testRecordProcessing() {
        collector.recordProcessing(FLOW_ID, NODE_ID, 100, true);
        
        NodeMetrics.Snapshot snapshot = collector.getNodeMetric(FLOW_ID, NODE_ID);
        assertNotNull(snapshot);
        assertEquals(1, snapshot.getProcessed());
    }

    @Test
    @DisplayName("에러 건수 기록: 실패로 기록 시 에러 카운트 증가")
    void testRecordError() {
        collector.recordProcessing(FLOW_ID, NODE_ID, 100, false);
        
        NodeMetrics.Snapshot snapshot = collector.getNodeMetric(FLOW_ID, NODE_ID);
        assertEquals(1, snapshot.getErrors());
    }

    @Test
    @DisplayName("평균 처리 시간: 여러 번 기록 후 평균 처리 시간 계산이 정확함")
    void testAverageProcessingTime() {
        collector.recordProcessing(FLOW_ID, NODE_ID, 100, true);
        collector.recordProcessing(FLOW_ID, NODE_ID, 200, true);
        
        NodeMetrics.Snapshot snapshot = collector.getNodeMetric(FLOW_ID, NODE_ID);
        assertEquals(150.0, snapshot.getAvgProcessingTime());
    }

    @Test
    @DisplayName("멀티스레드 안전성: 10개 스레드에서 동시에 기록해도 카운트가 정확함")
    void testMultithreadSafety() throws InterruptedException {
        int numberOfThreads = 10;
        int recordsPerThread = 1000;
        
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);
        
        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                for (int j = 0; j < recordsPerThread; j++) {
                    collector.recordProcessing(FLOW_ID, NODE_ID, 10, true);
                }
                latch.countDown();
            });
        }
        
        latch.await();
        executorService.shutdown();
        
        NodeMetrics.Snapshot snapshot = collector.getNodeMetric(FLOW_ID, NODE_ID);
        assertEquals(numberOfThreads * recordsPerThread, snapshot.getProcessed());
    }

    @Test
    @DisplayName("노드별 분리: 서로 다른 노드의 메트릭이 독립적으로 관리됨")
    void testNodeSeparation() {
        collector.recordProcessing(FLOW_ID, "node-1", 100, true);
        collector.recordProcessing(FLOW_ID, "node-2", 200, false);
        
        NodeMetrics.Snapshot snapshot1 = collector.getNodeMetric(FLOW_ID, "node-1");
        NodeMetrics.Snapshot snapshot2 = collector.getNodeMetric(FLOW_ID, "node-2");
        
        assertEquals(1, snapshot1.getProcessed());
        assertEquals(0, snapshot1.getErrors());
        
        assertEquals(1, snapshot2.getProcessed());
        assertEquals(1, snapshot2.getErrors());
    }

    @Test
    @DisplayName("리셋: 메트릭 초기화 후 데이터 없음")
    void testReset() {
        collector.recordProcessing(FLOW_ID, NODE_ID, 100, true);
        collector.resetFlowMetrics(FLOW_ID);
        
        NodeMetrics.Snapshot snapshot = collector.getNodeMetric(FLOW_ID, NODE_ID);
        assertNull(snapshot);
        
        List<NodeMetrics.Snapshot> flowMetrics = collector.getFlowMetrics(FLOW_ID);
        assertTrue(flowMetrics.isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 노드: 미등록 노드 id로 조회 시 빈 메트릭 또는 null")
    void testNonExistentNode() {
        NodeMetrics.Snapshot snapshot = collector.getNodeMetric(FLOW_ID, "non-existent");
        assertNull(snapshot);
    }
}
