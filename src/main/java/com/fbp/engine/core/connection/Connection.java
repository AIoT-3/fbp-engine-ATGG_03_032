package com.fbp.engine.core.connection;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class Connection {
    static final int DEFAULT_BUFFER_SIZE=100;

    private final String id;
    private final BlockingQueue<Message> buffer;
    private InputPort target;

    public Connection(String id) {
        this(id,DEFAULT_BUFFER_SIZE);
    }

    public Connection(String id, int buffer_size){
        if(id == null || id.isBlank()){
            throw new IllegalArgumentException("id must be notBlank");
        }
        if(buffer_size<=0){
            throw new IllegalArgumentException("buffer_size must be more than 0");
        }

        this.id = id;
        this.buffer = new LinkedBlockingQueue<>(buffer_size);
    }

    public void deliver(Message message){
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }


        try {
            buffer.put(message);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public Message poll(){
        Message message;
        try {
            message = buffer.poll(1000, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return message;
    }

    public void setTarget(InputPort target){
        Objects.requireNonNull(target, "target msut be notNull");

        this.target = target;
    }

    public int getBufferSize(){
        return this.buffer.size();
    }

    public String getId(){
        return this.id;
    }
}
