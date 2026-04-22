package com.fbp.engine.node.external;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fbp.engine.core.node.ProtocolNode;
import com.fbp.engine.message.Message;
import com.fbp.engine.message.MessageListener;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.mqttv5.client.IMqttMessageListener;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttSubscription;

import java.time.LocalDateTime;
import java.util.Map;

/*
config key::
brokerUrl(String, 예: "tcp://localhost:1883"),
clientId(String),
topic(String),
qos(int, 기본 1)
 */
@Slf4j
public class MqttSubscriberNode extends ProtocolNode implements MessageListener {
    private static final int DEFAULT_QOS = 1;

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private MqttClient client;

    public MqttSubscriberNode(String id, Map<String, Object> config) {
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

        addOutputPort("out");
    }

    @Override
    protected void connect() throws Exception {
        this.client = new MqttClient((String) getConfig("brokerUrl"), (String) getConfig("clientId"));

        MqttConnectionOptions options = new MqttConnectionOptions();
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);

        client.connect(options);

        MqttSubscription subscription = new MqttSubscription((String) getConfig("topic"),
                getConfig("qos")!=null? (int) getConfig("qos") :DEFAULT_QOS);
        IMqttMessageListener listener = (t, mqttMessage) -> {
            Map<String, Object> payload = processPayload(t, mqttMessage.getPayload());
            Message message = new Message(payload);
            send("out", message);
        };

        client.subscribe(new MqttSubscription[]{subscription}, new IMqttMessageListener[]{listener});
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
        throw new UnsupportedOperationException("[Node:" + getId() + "] Source-only node");
    }

    @Override
    public void onMessage(String json) {
        throw new UnsupportedOperationException("[Node:" + getId() + "] Use internal call back listener");
    }

    protected Map<String, Object> processPayload(String topic, byte[] rawPayload) {
        String json = new String(rawPayload);
        Map<String, Object> payload;
        try {
            payload = mapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            payload = new java.util.HashMap<>(Map.of("rawPayload", json));
        }
        //TODO... 요구사항 명세에는 "topic" 키를 이용하라고 되어있으나 논리적인 충돌로 인해 임시로 변경
        payload.put("sourceTopic", topic);
        payload.put("mqttTimestamp", LocalDateTime.now());
        return payload;
    }
}
