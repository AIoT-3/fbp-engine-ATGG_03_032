package com.fbp.engine.core.engine;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.core.node.Node;
import com.fbp.engine.core.parser.ConnectionDefinition;
import com.fbp.engine.core.parser.FlowDefinition;
import com.fbp.engine.core.parser.NodeDefinition;
import com.fbp.engine.core.registry.NodeRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class FlowManager {
    private final NodeRegistry nodeRegistry;

    private final Map<String,FlowEngine> flowEngines = new ConcurrentHashMap<>();

    public FlowManager() {
        nodeRegistry = new NodeRegistry();
    }

    public FlowManager(NodeRegistry nodeRegistry){
        if(nodeRegistry == null){
            throw new IllegalArgumentException("nodeRegistry must be notNull");
        }

        this.nodeRegistry = nodeRegistry;
    }

    public void deploy(FlowDefinition flowDefinition){
        if(flowDefinition == null){
            throw new IllegalArgumentException("flowDefinition must be notNull");
        }

        if(flowEngines.containsKey(flowDefinition.getId())){
            throw new IllegalArgumentException("flowId duplicated id:" + flowDefinition.getId());
        }

        Flow flow = new Flow(flowDefinition.getId());

        List<Node> nodes = new ArrayList<>();
        for(NodeDefinition nodeDefinition: flowDefinition.getNodes()){
            log.debug(nodeDefinition.getId() + " " + nodeDefinition.getType() + " " + nodeDefinition.getConfig());
            if(!nodeRegistry.isRegistered(nodeDefinition.getType())){
                throw new IllegalArgumentException("nodeRegistry not contain typeName:" + nodeDefinition.getType());
            }

            Node node = nodeRegistry.create(
                    nodeDefinition.getId(),
                    nodeDefinition.getType(),
                    nodeDefinition.getConfig());
            nodes.add(node);
        }
        nodes.stream().forEach(node -> flow.addNode((AbstractNode) node));

        for(ConnectionDefinition connectionDefinition: flowDefinition.getConnections()){
            String[] splitFrom = connectionDefinition.getFrom().split(":");
            String[] splitTo = connectionDefinition.getTo().split(":");

            flow.connect(splitFrom[0], splitFrom[1], splitTo[0], splitTo[1]);
        }

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.startFlow(flow.getId());

        flowEngines.put(flowDefinition.getId(), flowEngine);
    }

    public List<String> getDeployedFlowList(){
        return new ArrayList<>(flowEngines.keySet());
    }

    public State getStatus(String flowId){
        if (flowEngines.containsKey(flowId)) {
            return flowEngines.get(flowId).getState();
        }
        throw new IllegalArgumentException("flowManager not contain flowId:" + flowId);
    }

    public void stop(String flowId){
        if (flowEngines.containsKey(flowId)) {
            flowEngines.get(flowId).stopFlow(flowId);
            return;
        }
        throw new IllegalArgumentException("flowManager not contain flowId:" + flowId);
    }

    public void restart(String flowId){
        if(flowEngines.containsKey(flowId)){
            flowEngines.get(flowId).startFlow(flowId);
            return;
        }
        throw new IllegalArgumentException("flowManager not contain flowId:" + flowId);
    }

    public void remove(String flowId){
        if(flowEngines.containsKey(flowId)){
            FlowEngine flowEngine = flowEngines.get(flowId);
            flowEngine.shutdown();

            flowEngines.remove(flowId);
            return;
        }
        throw new IllegalArgumentException("flowManager not contain flowId:" + flowId);
    }
}
