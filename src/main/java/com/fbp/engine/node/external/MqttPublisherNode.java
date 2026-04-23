package com.fbp.engine.node.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fbp.engine.core.node.ProtocolNode;
import com.fbp.engine.message.Message;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

/*
config key::
brokerUrl(String),
clientId(String),
topic(String, 기본 발행 토픽),
qos(int, 기본 1),
retained(boolean, 기본 false)
 */
@Slf4j
public class MqttPublisherNode extends ProtocolNode {
    private static final int DEFAULT_QOS = 1;
    private static final boolean DEFAULT_RETAINED = false;

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private MqttClient client;

    @Getter
    private long errorCount = 0;

    public MqttPublisherNode(String id, Map<String, Object> config) {
        super(id, config);

        String brokerUrl = (String) config.get("brokerUrl");
        String clientId = (String) config.get("clientId");
        String topic = (String) config.get("topic");

        if(brokerUrl==null){
            throw new IllegalArgumentException("brokerUrl must be notNull");
        }
        if(clientId==null){
            throw new IllegalArgumentException("clientId must be notNull");
        }
        if(topic==null){
            throw new IllegalArgumentException("topic must be notNull");
        }

        addInputPort("in");
    }

    @Override
    protected void connect() throws Exception {
        log.debug("client connecting...");
        this.client = new MqttClient((String) getConfig("brokerUrl"),
                (String) getConfig("clientId"),
                new MemoryPersistence());

        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);

        client.connect(options);

        log.debug("client connected");
    }

    @Override
    protected void disconnect() {
        if(client!=null && client.isConnected()){
            try {
                client.disconnect();
                client.close();
            } catch (MqttException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @Override
    public void onProcess(String portName, Message message) {
        byte[] jsonByte;
        try {
            jsonByte = mapper.writeValueAsBytes(message.getPayload());
        } catch (JsonProcessingException e) {
            log.error(e.getMessage(),e);
            errorCount++;
            throw new RuntimeException(e);
        }

        //TODO... 요구사항 명세에는 "topic" 키를 이용하라고 되어있으나 논리적인 충돌로 인해 임시로 변경
        String topic = message.get("targetTopic");
        if(topic==null){
            topic = (String) getConfig("topic");
        }

        MqttMessage mqttMessage = new MqttMessage(jsonByte);
        mqttMessage.setQos(getConfig("qos") != null?
                (int) getConfig("qos") : DEFAULT_QOS);
        mqttMessage.setRetained(getConfig("retained") != null?
                (boolean)getConfig("retained") : DEFAULT_RETAINED);

        try {
            client.publish(topic,mqttMessage);
        } catch (MqttException e) {
            log.error(e.getMessage(),e);
            errorCount++;
            throw new RuntimeException(e);
        }
    }
}
