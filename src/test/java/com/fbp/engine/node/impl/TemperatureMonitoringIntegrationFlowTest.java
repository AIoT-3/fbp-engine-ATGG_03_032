package com.fbp.engine.node.impl;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;

import java.util.Map;

public class TemperatureMonitoringIntegrationFlowTest {
    Flow flow;
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
    }

    @Order(1)
    @Test
    @DisplayName("alert 경로 검증")
    void checkAlertPath(){
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        trigger.shutdown();

        for(Message message:alert.getCollected()){
            if(message!=null){
                Assertions.assertTrue((Double)(message.get("temperature"))>threshold);
            }
        }
    }

    @Order(2)
    @Test
    @DisplayName("normal 경로 검증")
    void checkNormalPath(){
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        flow.shutdown();

        for(Message message:normal.getCollected()){
            if(message!=null){
                Assertions.assertTrue((Double)(message.get("temperature"))<=threshold);
            }
        }
    }

    @Order(3)
    @Test
    @DisplayName("전체 메시지 수")
    void checkTotalMessageCount(){
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        trigger.shutdown();

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        flow.shutdown();

        Assertions.assertEquals(trigger.getTickCount(),
                normal.getCollected().size()+alert.getCollected().size());
    }


}
