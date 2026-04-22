package com.fbp.engine;

import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;

public class Main_12_2 {
    public static void main(String[] args) throws MqttException, InterruptedException {
        String broker = "tcp://localhost:1883";
        String clientId = "client-id";
        String topic = "sensor/temperature";

        MqttClient client = new MqttClient(broker, clientId);
        // 연결 설정
        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);

        System.out.println("브로커 연결 중: " + broker);
        client.connect(options);

        // 구독 설정 (1.2.5 버전의 StackOverflowError를 피하기 위해 배열 방식 사용)
        MqttSubscription subscription = new MqttSubscription(topic, 1);
        IMqttMessageListener listener = (t, msg) -> {
            String payload = new String(msg.getPayload());
            System.out.println("수신 성공! [" + t + "] → " + payload);
        };

        System.out.println("토픽 구독 중: " + topic);
        client.subscribe(new MqttSubscription[]{subscription}, new IMqttMessageListener[]{listener});

        // 메시지 발행
        String content = "25.5";
        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(1);

        System.out.println("메시지 발행 중: " + content);
        client.publish(topic, message);

        // 처리 시간 동안 대기
        Thread.sleep(1000);

        // 연결 해제
        client.disconnect();
        System.out.println("연결 해제 완료.");


    }
}