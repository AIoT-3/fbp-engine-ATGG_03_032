package com.fbp.engine.core.node;

import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.core.port.OutputPort;
import com.fbp.engine.core.port.impl.DefaultInputPort;
import com.fbp.engine.core.port.impl.DefaultOutputPort;
import com.fbp.engine.message.ErrorMessage;
import com.fbp.engine.message.Message;
import com.fbp.engine.metrics.MetricsCollector;
import com.fbp.engine.node.exception.NotFoundPortNameException;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public abstract class AbstractNode implements Node {
    
    private static final AtomicBoolean globalDebugMode = new AtomicBoolean(false);

    private final String id;
    private final Map<String, InputPort> inputPorts;
    private final Map<String, OutputPort> outputPorts;

    private String flowId;

    public AbstractNode(String id) {
        if(id == null || id.isBlank()){
            throw new IllegalArgumentException("id must be notBlank");
        }
        this.id = id;
        this.inputPorts = new HashMap<>();
        this.outputPorts  = new HashMap<>();
        
        addOutputPort("_error");
    }

    public static void setGlobalDebugMode(boolean enabled) {
        globalDebugMode.set(enabled);
        log.warn("Global debug mode has been " + (enabled ? "ENABLED" : "DISABLED"));
    }

    public static boolean isGlobalDebugModeEnabled() {
        return globalDebugMode.get();
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getFlowId() {
        return flowId != null ? flowId : "UNKNOWN_FLOW";
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

    public void send(String portName, Message message){
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
        long startTime = System.currentTimeMillis();
        boolean success = false;

        if (globalDebugMode.get()) {
            log.info("[{}] processing message: {}", getId(), message);
        }

        try {
            onProcess(portName, message);
            success = true;
        } catch (Exception e) {
            log.error("[{}], error during processing on port '{}': {}", getId(), portName, e.getMessage(), e);
            success = false;
            
            OutputPort errorPort = getOutputPort("_error");
            if (errorPort != null && errorPort.isConnected()) {
                ErrorMessage errorMsg = new ErrorMessage(message, e, getId());
                send("_error", errorMsg);
            }
        } finally {
            long endTime = System.currentTimeMillis();
            MetricsCollector.getInstance().recordProcessing(getFlowId(), getId(), endTime-startTime, success);
        }
    }

    public String getId(){return id;}
}
