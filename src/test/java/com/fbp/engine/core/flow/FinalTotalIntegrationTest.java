package com.fbp.engine.core.flow;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.internal.*;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

public class FinalTotalIntegrationTest {
    FlowEngine flowEngine;
    Flow flow;

    TimerNode timerNode;
    TemperatureSensorNode temperatureSensorNode;
    ThresholdFilterNode thresholdFilterNode;

    AlertNode alertNode;
    CollectorNode alertCollector;

    LogNode logNode;
    CollectorNode normalCollector;
    String filePath;
    FileWriterNode fileWriterNode;

    @BeforeEach
    void setUp(){
        flowEngine = new FlowEngine();
        flow = new Flow("flow");
        timerNode = new TimerNode("trigger",100);
        temperatureSensorNode = new TemperatureSensorNode("sensor-temperature", 15, 45);
        thresholdFilterNode = new ThresholdFilterNode("filter-temperature", "temperature", 30);
        alertNode = new AlertNode("alerter");
        alertCollector = new CollectorNode("collector-alert");
        logNode = new LogNode("logger");
        normalCollector = new CollectorNode("collector-normal");
        filePath = "submit/" + UUID.randomUUID().toString() + ".txt";
        fileWriterNode = new FileWriterNode("writer-file", filePath);

        flowEngine.register(flow);

        flow.addNode(timerNode)
                .addNode(temperatureSensorNode)
                .addNode(thresholdFilterNode)
                .addNode(alertNode)
                .addNode(alertCollector)
                .addNode(logNode)
                .addNode(normalCollector)
                .addNode(fileWriterNode)
                .connect(timerNode.getId(),"out", temperatureSensorNode.getId(),"trigger")
                .connect(temperatureSensorNode.getId(),"out", thresholdFilterNode.getId(), "in")
                .connect(thresholdFilterNode.getId(), "alert", alertNode.getId(), "in")
                .connect(thresholdFilterNode.getId(), "alert", alertCollector.getId(), "in")
                .connect(thresholdFilterNode.getId(), "normal", logNode.getId(), "in")
                .connect(thresholdFilterNode.getId(), "normal", fileWriterNode.getId(), "in")
                .connect(thresholdFilterNode.getId(), "normal", normalCollector.getId(), "in");

        flowEngine.startFlow(flow.getId());
    }

    @Order(1)
    @Test
    @DisplayName("엔진 시작/종료")
    void engineStartAndStop(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Assertions.assertEquals(State.RUNNING, flowEngine.getState());

        flowEngine.shutdown();

        Assertions.assertEquals(State.STOPPED, flowEngine.getState());
    }

    @Order(2)
    @Test
    @DisplayName("alert 경로 정확성")
    void checkAlertPath(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        flowEngine.shutdown();

        for(Message message: alertCollector.getCollected()){
            Assertions.assertTrue((Double)(message.get("temperature"))>30);
        }
    }

    @Order(3)
    @Test
    @DisplayName("normal 경로 정확성")
    void checkNormalPath(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        flowEngine.shutdown();

        for(Message message: normalCollector.getCollected()){
            Assertions.assertTrue((Double)(message.get("temperature"))<=30);
        }
    }

    @Order(4)
    @Test
    @DisplayName("전체 분기 완전성")
    void checkAllPath(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        timerNode.shutdown();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        flowEngine.shutdown();

        Assertions.assertEquals(timerNode.getTickCount(),
                alertCollector.getCollected().size() + normalCollector.getCollected().size());
    }

    @Order(5)
    @Test
    @DisplayName("파일 기록 검증")
    void checkWroteFile(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        timerNode.shutdown();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        flowEngine.shutdown();

        int lineCount=0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            String s=null;
            while ((s=bufferedReader.readLine())!=null){
                lineCount++;
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Assertions.assertEquals(normalCollector.getCollected().size(), lineCount);
    }

    @Order(6)
    @Test
    @DisplayName("센서 데이터 형식")
    void sensorDateFormat(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        timerNode.shutdown();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        flowEngine.shutdown();

        for(Message message: alertCollector.getCollected()){
            Assertions.assertAll(
                    () -> Assertions.assertTrue(message.hasKey("sensorId")),
                    () -> Assertions.assertTrue(message.hasKey("temperature")),
                    () -> Assertions.assertTrue(message.hasKey("unit"))
            );
        }

        for(Message message: normalCollector.getCollected()){
            Assertions.assertAll(
                    () -> Assertions.assertTrue(message.hasKey("sensorId")),
                    () -> Assertions.assertTrue(message.hasKey("temperature")),
                    () -> Assertions.assertTrue(message.hasKey("unit"))
            );
        }
    }

    @Order(7)
    @Test
    @DisplayName("온도 범위")
    void checkTemperatureRange(){
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        timerNode.shutdown();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        flowEngine.shutdown();

        for(Message message: alertCollector.getCollected()){
            Double v = message.get("temperature");
            Assertions.assertTrue(15.0<=v && v<=45.0);
        }

        for(Message message: normalCollector.getCollected()){
            Double v = message.get("temperature");
            Assertions.assertTrue(15.0<=v && v<=45.0);
        }
    }

}
