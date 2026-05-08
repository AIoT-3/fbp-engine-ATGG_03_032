package com.fbp.engine.integration;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.internal.CollectorNode;
import com.fbp.engine.node.internal.TemperatureSensorNode;
import com.fbp.engine.node.internal.ThresholdFilterNode;
import com.fbp.engine.node.internal.TimerNode;
import org.junit.jupiter.api.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TemperatureMonitoringIntegrationFlowTest {
    Flow flow;
    ExecutorService executorService;

    TimerNode trigger;
    TemperatureSensorNode temperatureSensorNode;
    double threshold = 35;
    ThresholdFilterNode thresholdFilterNode;
    CollectorNode alert;
    CollectorNode normal;

    @BeforeEach
    void setUp() {
        flow = new Flow("flow");
        trigger = new TimerNode("trigger", 50);
        temperatureSensorNode = new TemperatureSensorNode("temperature", 25, 40);
        thresholdFilterNode = new ThresholdFilterNode("temperature-threshold", "temperature", threshold);
        alert = new CollectorNode("alert-collector");
        normal = new CollectorNode("normal-collector");

        flow.addNode(trigger)
                .addNode(temperatureSensorNode)
                .addNode(thresholdFilterNode)
                .addNode(alert)
                .addNode(normal)
                .connect(trigger.getId(), "out", temperatureSensorNode.getId(), "trigger")
                .connect(temperatureSensorNode.getId(), "out", thresholdFilterNode.getId(), "in")
                .connect(thresholdFilterNode.getId(), "alert", alert.getId(), "in")
                .connect(thresholdFilterNode.getId(), "normal", normal.getId(), "in");

        flow.initialize();

        executorService = Executors.newCachedThreadPool();
        flow.getConnections().forEach(conn ->
                executorService.submit(() -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        conn.poll();
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                })
        );
    }

    @AfterEach
    void tearDown() {
        flow.shutdown();
        executorService.shutdownNow();
    }

    @Order(1)
    @Test
    @DisplayName("alert 경로 검증")
    void checkAlertPath() throws InterruptedException {
        Thread.sleep(2000);
        trigger.shutdown();

        for (Message message : alert.getCollected()) {
            if (message != null) {
                Assertions.assertTrue((Double) (message.get("temperature")) > threshold);
            }
        }
    }

    @Order(2)
    @Test
    @DisplayName("normal 경로 검증")
    void checkNormalPath() throws InterruptedException {
        Thread.sleep(2000);

        for (Message message : normal.getCollected()) {
            if (message != null) {
                Assertions.assertTrue((Double) (message.get("temperature")) <= threshold);
            }
        }
    }

    @Order(3)
    @Test
    @DisplayName("전체 메시지 수")
    void checkTotalMessageCount() throws InterruptedException {
        Thread.sleep(2000);
        trigger.shutdown();
        Thread.sleep(500);

        Assertions.assertEquals(trigger.getTickCount(),
                normal.getCollected().size() + alert.getCollected().size());
    }
}