package com.fbp.engine.core.connection;

import com.fbp.engine.core.connection.strategy.BackpressureStrategy;
import com.fbp.engine.core.connection.strategy.BlockStrategy;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class Connection {
    static final int DEFAULT_BUFFER_SIZE=100;

    private final String id;
    private final BlockingQueue<Message> buffer;
    private InputPort target;

    private volatile BackpressureStrategy strategy;
    private final AtomicLong droppedMessages = new AtomicLong(0);

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
        this.strategy = new BlockStrategy();
    }

    public void deliver(Message message){
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }

        try {
            strategy.offer(message, buffer, droppedMessages);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public Message poll(){
        try {
            Message message = buffer.take();
            if(target != null){
                target.receive(message);
            }
            return message;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public void setTarget(InputPort inputPort){
        this.target=inputPort;
    }
    
    public void setStrategy(BackpressureStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("strategy must be notNull");
        }
        this.strategy = strategy;
    }
    
    public long getDroppedMessages() {
        return droppedMessages.get();
    }

    public int getBufferSize(){
        return buffer.size();
    }

    public String getId(){
        return id;
    }
}
