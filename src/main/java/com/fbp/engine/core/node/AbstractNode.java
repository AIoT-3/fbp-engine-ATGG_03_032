package com.fbp.engine.core.node;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.core.port.OutputPort;
import com.fbp.engine.core.port.impl.DefaultInputPort;
import com.fbp.engine.core.port.impl.DefaultOutputPort;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.exception.NotFoundPortNameException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public abstract class AbstractNode implements Node {
    private final String id;
    private final Map<String, InputPort> inputPorts;
    private final Map<String, OutputPort> outputPorts;

    public AbstractNode(String id) {
        if(id == null || id.isBlank()){
            throw new IllegalArgumentException("id must be notBlank");
        }
        this.id = id;
        this.inputPorts = new HashMap<>();
        this.outputPorts  = new HashMap<>();
    }

    @Override
    public void initialize() {
        log.info(String.format("[%s], initializing", id));
    }

    @Override
    public void shutdown() {
        log.info(String.format("[%s], shutdown", id));
    }

    protected void addInputPort(String name){
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("name must be notBlank");
        }
        inputPorts.put(name, new DefaultInputPort(name,this));
    }

    protected void addOutputPort(String name){
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("name must be notBlank");
        }
        outputPorts.put(name, new DefaultOutputPort(name));
    }

    public InputPort getInputPort(String name){
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("name must be notBlank");
        }

        return inputPorts.get(name);
    }
    public OutputPort getOutputPort(String name){
        if(name == null || name.isBlank()){
            throw new IllegalArgumentException("name must be notBlank");
        }

        return outputPorts.get(name);
    }

    protected void send(String portName, Message message){
        if(portName == null || portName.isBlank()){
            throw new IllegalArgumentException("portName must be notBlank");
        }
        if(!outputPorts.containsKey(portName)){
            throw new NotFoundPortNameException(portName);
        }
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }
        outputPorts.get(portName).send(message);
    }

    public abstract void onProcess(String portName, Message message);

    @Override
    public void process(String portName, Message message) {
        log.info("[{}], processing message...", getId());
        try {
            onProcess(portName, message);
            log.info("[{}], ...Processing message completed", getId());
        } catch (Exception e) {
            log.error("[{}], error during processing on port '{}': {}", getId(), portName, e.getMessage(), e);
            throw e;
        }
    }

    public String getId(){return id;}
}
