package com.fbp.engine.core.connection;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;
import lombok.Setter;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
            Thread.currentThread().interrupt();
        }
    }

    public Message poll(){
        Message message = null;

        try {
            message = buffer.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        if(target != null){
            target.receive(message);
        }

        return message;
    }

    public void setTarget(InputPort inputPort){
        this.target=inputPort;
    }

    public int getBufferSize(){
        return buffer.size();
    }

    public String getId(){
        return id;
    }
}
