package com.fbp.engine.message;

import com.fbp.engine.message.exception.DuplicatePayloadKeyException;
import com.fbp.engine.message.exception.NotFoundPayloadKeyException;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@ToString
public class Message {
    //UUID
    private final String id;
    private final Map<String, Object> payload;
    private final LocalDateTime timestamp;

    public Message(final Map<String, Object> payload){
        if(payload == null || payload.isEmpty()){
            throw new IllegalArgumentException("payload must be notEmpty");
        }

        this.id = UUID.randomUUID().toString();
        this.payload = Map.copyOf(payload);
        this.timestamp = LocalDateTime.now();
    }

    public <T> T get(String key){
        if(key == null || key.isBlank()){
            throw new IllegalArgumentException("key must be notBlank");
        }
        if(!payload.containsKey(key)){
            throw new NotFoundPayloadKeyException(key);
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

//        if(payload.containsKey(key)){
//            throw new DuplicatePayloadKeyException(key);
//        }

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
}
