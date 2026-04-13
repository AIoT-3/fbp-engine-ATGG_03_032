package com.fbp.engine.node.impl;

import com.fbp.engine.message.Message;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class AlertNode extends AbstractNode{
    public AlertNode(String id) {
        super(id);

        addInputPort("in");
    }

    @Override
    public void onProcess(String portName, Message message) {
        String time = DateTimeFormatter.ofPattern("HH:mm:ss.SSS").format(LocalTime.now());
        if (message == null) {
            throw new IllegalArgumentException("message must be notNull");
        }

        String sensorId = message.get("sensorId");
        Double temperature = message.get("temperature");
        Double humidity = message.get("humidity");

        if (sensorId == null || temperature == null && humidity == null) {
            System.out.println(String.format(
                    "[%s][%s] [Warning] Unknown sensor data",
                    getId(), time)
            );
            return;
        }

        if (sensorId != null && temperature != null) {
            System.out.println(String.format(
                    "[%s][%s] [Warning] SensorId:%s temperature:%f°C — Threshold exceeded!",
                    getId(),time,sensorId, temperature));
        }

        if (sensorId != null && humidity != null) {
            System.out.println(String.format(
                    "[%s][%s] [Warning] SensorId:%s humidity:%f%% — Threshold exceeded!",
                    getId(),time, sensorId, humidity));
        }
    }
}
