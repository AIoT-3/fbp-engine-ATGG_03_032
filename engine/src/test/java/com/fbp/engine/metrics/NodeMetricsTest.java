package com.fbp.engine.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NodeMetricsTest {

    private NodeMetrics nodeMetrics;
    private static final String NODE_ID = "test-node";

    @BeforeEach
    void setUp() {
        nodeMetrics = new NodeMetrics(NODE_ID);
    }

    @Test
    @DisplayName("초기값: 생성 직후 모든 카운터가 0")
    void testInitialValues() {
        NodeMetrics.Snapshot snapshot = nodeMetrics.getSnapshot();
        assertEquals(NODE_ID, snapshot.getNodeId());
        assertEquals(0, snapshot.getProcessed());
        assertEquals(0, snapshot.getErrors());
        assertEquals(0.0, snapshot.getAvgProcessingTime());
    }

    @Test
    @DisplayName("increment: 처리 건수, 에러 건수 증가")
    void testIncrementCounts() {
        nodeMetrics.record(100, true);
        NodeMetrics.Snapshot snapshot1 = nodeMetrics.getSnapshot();
        assertEquals(1, snapshot1.getProcessed());
        assertEquals(0, snapshot1.getErrors());

        nodeMetrics.record(150, false);
        NodeMetrics.Snapshot snapshot2 = nodeMetrics.getSnapshot();
        assertEquals(2, snapshot2.getProcessed());
        assertEquals(1, snapshot2.getErrors());
    }

    @Test
    @DisplayName("평균 계산: 처리 시간 합계 / 처리 건수 = 평균")
    void testAverageCalculation() {
        nodeMetrics.record(100, true);
        nodeMetrics.record(200, true);
        nodeMetrics.record(300, false);

        NodeMetrics.Snapshot snapshot = nodeMetrics.getSnapshot();
        assertEquals(3, snapshot.getProcessed());
        assertEquals((100 + 200 + 300) / 3.0, snapshot.getAvgProcessingTime());
    }

    @Test
    @DisplayName("스냅샷: 현재 메트릭의 불변 스냅샷 반환")
    void testSnapshotImmutability() {
        nodeMetrics.record(100, true);
        NodeMetrics.Snapshot snapshot1 = nodeMetrics.getSnapshot();

        nodeMetrics.record(200, false);

        assertEquals(1, snapshot1.getProcessed());
        assertEquals(0, snapshot1.getErrors());
        assertEquals(100.0, snapshot1.getAvgProcessingTime());

        NodeMetrics.Snapshot snapshot2 = nodeMetrics.getSnapshot();
        assertEquals(2, snapshot2.getProcessed());
        assertEquals(1, snapshot2.getErrors());
    }

    @Test
    @DisplayName("리셋: 메트릭 초기화 후 카운트가 0")
    void testReset() {
        nodeMetrics.record(100, true);
        nodeMetrics.record(200, false);

        nodeMetrics.reset();

        NodeMetrics.Snapshot snapshot = nodeMetrics.getSnapshot();
        assertEquals(0, snapshot.getProcessed());
        assertEquals(0, snapshot.getErrors());
        assertEquals(0.0, snapshot.getAvgProcessingTime());
    }
}
