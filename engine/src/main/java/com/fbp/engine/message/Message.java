package com.fbp.engine.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fbp.engine.message.exception.DuplicatePayloadKeyException;
import com.fbp.engine.message.exception.NotFoundPayloadKeyException;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Getter
public class Message {
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    //UUID
    private final UUID id;
    private final Map<String, Object> payload;
    private final LocalDateTime timestamp;

    public Message(final Map<String, Object> payload){
        if(payload == null || payload.isEmpty()){
            throw new IllegalArgumentException("payload must be notEmpty");
        }

        this.id = UUID.randomUUID();
        this.payload = Map.copyOf(payload);
        this.timestamp = LocalDateTime.now();
    }

    @JsonCreator
    public Message(
            @JsonProperty("id") UUID id,
            @JsonProperty("payload") Map<String, Object> payload,
            @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.id = id != null ? id : UUID.randomUUID();
        this.payload = payload != null ? Map.copyOf(payload) : Map.of();
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key){
        if(key == null || key.isBlank()){
            throw new IllegalArgumentException("key must be notBlank");
        }

        return (T) payload.get(key);
    }

    public Message withEntry(String key, Object value){
        if(key==null || key.isBlank()){
            throw new IllegalArgumentException("key must be notBlank");
        }
        if(value == null){
            throw new IllegalArgumentException("value must be notNull");
        }

        Map<String, Object> newPayload = new HashMap<>(Map.copyOf(payload));
        newPayload.put(key,value);
        return new Message(newPayload);
    }

    public Message withoutKey(String key){
        if(key==null || key.isBlank()){
            throw new IllegalArgumentException("key must be notBlank");
        }
        if(!payload.containsKey(key)){
            throw new NotFoundPayloadKeyException(key);
        }

        Map<String, Object> newPayload = new HashMap<>(Map.copyOf(payload));
        newPayload.remove(key);
        return new Message(newPayload);
    }

    public boolean hasKey(String key){
        if(key == null || key.isBlank()){
            throw new IllegalArgumentException("key must be notBlank");
        }

        return payload.containsKey(key);
    }

    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", payload=" + payload +
                ", timestamp=" + timestamp +
                '}';
    }

    public String toJson(){
        try {
            return mapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.warn("{}",e);
            throw new RuntimeException(e);
        }
    }

    public static Message fromJson(String json) {
        try {
            return mapper.readValue(json, Message.class);
        } catch (JsonProcessingException e) {
            log.warn("{}",e);
            throw new RuntimeException(e);
        }
    }
}
