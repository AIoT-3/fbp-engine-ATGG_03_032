package com.fbp.engine.core.connection;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

public class Connection {
    private final String id;
    private final Queue<Message> buffer;
    private InputPort target;

    public Connection(String id) {
        if(id == null || id.isBlank()){
            throw new IllegalArgumentException("id must be notBlank");
        }

        this.id = id;
        this.buffer = new LinkedList<>();
    }

    public void deliver(Message message){
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }

        buffer.add(message);

        if(target != null){
            target.receive(buffer.poll());
        }
    }

    public void setTarget(InputPort target){
        Objects.requireNonNull(target, "target msut be notNull");

        this.target = target;
    }

    public int getBufferSize(){
        return this.buffer.size();
    }
}
