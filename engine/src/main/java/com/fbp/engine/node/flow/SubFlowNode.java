package com.fbp.engine.node.flow;

import com.fbp.engine.core.engine.FlowEngine;
import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.core.parser.FlowDefinition;
import com.fbp.engine.core.parser.NodeDefinition;
import com.fbp.engine.core.registry.NodeRegistry;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class SubFlowNode extends AbstractNode {

    private final FlowDefinition subFlowDefinition;
    private final NodeRegistry nodeRegistry;
    private FlowEngine internalFlowEngine;

    private final Map<String, SubFlowInputNode> internalInputs = new HashMap<>();

    public SubFlowNode(String id, FlowDefinition subFlowDefinition, NodeRegistry nodeRegistry) {
        super(id);
        this.subFlowDefinition = subFlowDefinition;
        this.nodeRegistry = nodeRegistry; 

        if (subFlowDefinition.getPublicPorts() != null) {
            for (Map.Entry<String, String> entry : subFlowDefinition.getPublicPorts().entrySet()) {
                String publicPortName = entry.getKey();
                
                if (publicPortName.toLowerCase().contains("in")) {
                    addInputPort(publicPortName);
                    SubFlowInputNode inputProxy = new SubFlowInputNode("proxy_in_" + publicPortName);
                    internalInputs.put(publicPortName, inputProxy);
                } else {
                    addOutputPort(publicPortName);
                }
            }
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        Flow internalFlow = new Flow(getId() + "_internal_flow");
        internalFlowEngine = new FlowEngine();

        for (NodeDefinition nodeDef : subFlowDefinition.getNodes()) {
            AbstractNode node = (AbstractNode) nodeRegistry.create(nodeDef.getId(), nodeDef.getType(), nodeDef.getConfig());
            internalFlow.addNode(node);
        }

        internalInputs.values().forEach(internalFlow::addNode);

        if (subFlowDefinition.getPublicPorts() != null) {
            subFlowDefinition.getPublicPorts().forEach((publicPort, internalTarget) -> {
                if (!publicPort.toLowerCase().contains("in")) {
                    SubFlowOutputNode outputProxy = new SubFlowOutputNode("proxy_out_" + publicPort, publicPort, this::send);
                    internalFlow.addNode(outputProxy);
                }
            });
        }

        SubFlowOutputNode errorProxy = new SubFlowOutputNode("proxy_out_error", "_error", this::send);
        internalFlow.addNode(errorProxy);

        subFlowDefinition.getConnections().forEach(conn -> 
            internalFlow.connect(conn.getFrom().split(":")[0], conn.getFrom().split(":")[1],
                                 conn.getTo().split(":")[0], conn.getTo().split(":")[1],
                                 conn.getCapacity(), conn.getBackpressure())
        );

        if (subFlowDefinition.getPublicPorts() != null) {
            subFlowDefinition.getPublicPorts().forEach((publicPort, internalTarget) -> {
                String internalNodeId = internalTarget.split("\\.")[0];
                String internalPortName = internalTarget.split("\\.")[1];
                
                if (publicPort.toLowerCase().contains("in")) {
                    internalFlow.connect("proxy_in_" + publicPort, "out", internalNodeId, internalPortName);
                } else {
                    internalFlow.connect(internalNodeId, internalPortName, "proxy_out_" + publicPort, "in");
                }
            });
        }

        for (NodeDefinition nodeDef : subFlowDefinition.getNodes()) {
            internalFlow.connect(nodeDef.getId(), "_error", "proxy_out_error", "in");
        }

        internalFlowEngine.register(internalFlow);
        internalFlowEngine.startFlow(internalFlow.getId());
    }

    @Override
    public void shutdown() {
        if (internalFlowEngine != null) {
            internalFlowEngine.shutdown();
        }
        super.shutdown();
    }

    @Override
    public void onProcess(String portName, Message message) {
        SubFlowInputNode inputProxy = internalInputs.get(portName);
        if (inputProxy != null) {
            inputProxy.process("in", message);
        } else {
            log.warn("[{}] Received message on unmapped public port: {}", getId(), portName);
        }
    }
}
