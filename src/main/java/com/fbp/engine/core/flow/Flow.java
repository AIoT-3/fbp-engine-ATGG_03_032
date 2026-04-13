package com.fbp.engine.core.flow;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.core.port.OutputPort;
import com.fbp.engine.node.impl.AbstractNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class Flow {
    @Getter
    private final String id;

    private final Map<String, AbstractNode> nodes;
    private final List<Connection> connections;
    private ExecutorService executorService;

    @Getter
    private FlowEngine.State state;

    public Flow(String id) {
        if(id == null || id.isBlank()){
            throw new IllegalArgumentException("id must be notBlank");
        }
        this.id = id;

        nodes = new HashMap<>();
        connections = new ArrayList<>();

        state = FlowEngine.State.INITIALIZED;
    }

    public Flow addNode(AbstractNode node){
        if(node == null){
            throw new IllegalArgumentException("node must be notNull");
        }

        nodes.put(node.getId(),node);
        return this;
    }

    public Flow connect(String sourceNodeId, String sourcePort, String targetNodeId, String targetPort){
        if(sourceNodeId == null || sourceNodeId.isBlank()){
            throw new IllegalArgumentException("sourceNodeId must be notBlank");
        }

        if(sourcePort == null || sourcePort.isBlank()){
            throw new IllegalArgumentException("sourcePort must be notBlank");
        }

        if(targetNodeId == null || targetNodeId.isBlank()){
            throw new IllegalArgumentException("targetNodeId must be notBlank");
        }

        if(targetPort == null || targetPort.isBlank()){
            throw  new IllegalArgumentException("targetPort must be notBlank");
        }

        AbstractNode sourceNode = nodes.get(sourceNodeId);
        if(sourceNode == null){
            throw new IllegalArgumentException(String.format("flow not contains node... id: %s", sourceNodeId));
        }
        AbstractNode targetNode = nodes.get(targetNodeId);
        if(targetNode == null){
            throw new IllegalArgumentException(String.format("flow not contains node.. id: %s.", targetNodeId));
        }

        OutputPort outputPort = sourceNode.getOutputPort(sourcePort);
        if(outputPort == null){
            throw new IllegalArgumentException(String.format("node:%s not founded Output fort:%s", sourceNodeId, sourcePort));
        }

        InputPort inputPort = targetNode.getInputPort(targetPort);
        if(inputPort == null){
            throw new IllegalArgumentException(String.format("node:%s not founded Input fort:%s", targetNodeId, targetPort));
        }

        Connection connection = new Connection(String.format("%s:%s->%s:%s", sourceNodeId, sourcePort, targetNodeId, targetPort));
        sourceNode.getOutputPort(sourcePort).connect(connection);
        connection.setTarget(targetNode.getInputPort(targetPort));

        connections.add(connection);

        return this;
    }

    public void initialize(){
        if(this.state == FlowEngine.State.RUNNING){
            return;
        }
        for(AbstractNode node: nodes.values()){
            node.initialize();
        }

        executorService = Executors.newCachedThreadPool();

        connections.forEach(connection -> {
            executorService.submit(()->{
                while(!Thread.currentThread().isInterrupted()){
                    connection.poll();
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e){
                        log.error("Error polling connection: " + connection.getId(), e);
                    }
                }
            });
        });

        this.state = FlowEngine.State.RUNNING;
    }

    public void shutdown(){
        for(AbstractNode node: nodes.values()){
            node.shutdown();
        }

        if(executorService != null) {
            executorService.shutdownNow();
        }
        this.state = FlowEngine.State.STOPPED;
    }

    public List<AbstractNode> getNodes(){
        return nodes.values().stream().toList();
    }

    public List<Connection> getConnections(){
        return new ArrayList<>(connections);
    }

    public List<String> validate() {
        List<String> errs = new ArrayList<>();

        checkNodeCount(errs);
        checkConnection(errs);
        checkCircularReference(errs);

        return errs;
    }

    private void checkNodeCount(List<String> errs){
        if (nodes.isEmpty()) {
            errs.add("Flow validation failed: The flow must contain at least one node.");
        }
    }

    private void checkConnection(List<String> errs){
        for (Connection connection : connections) {
            String connectionId = connection.getId();
            String[] split = connectionId.split("->");

            if (split.length != 2) {
                errs.add(String.format("Invalid connection format: [%s]. Connection ID must follow the pattern 'source:port->target:port'.", connectionId));
                continue;
            }

            String[] sourceParts = split[0].split(":");
            if (sourceParts.length < 1 || sourceParts[0].isBlank()) {
                errs.add(String.format("Connection [%s] error: Source node ID is missing.", connectionId));
            }
            String sourceNodeId = sourceParts[0];

            String[] targetParts = split[1].split(":");
            if (targetParts.length < 1 || targetParts[0].isBlank()) {
                errs.add(String.format("Connection [%s] error: Target node ID is missing.", connectionId));
            }
            String targetNodeId = targetParts[0];

            if (!nodes.containsKey(sourceNodeId)) {
                errs.add(String.format("Orphaned connection: Connection [%s] refers to a source node [%s] that does not exist in the flow.", connectionId, sourceNodeId));
            }

            if (!nodes.containsKey(targetNodeId)) {
                errs.add(String.format("Orphaned connection: Connection [%s] refers to a target node [%s] that does not exist in the flow.", connectionId, targetNodeId));
            }
        }
    }
    private void checkCircularReference(List<String> errs){
        Map<String, List<String>> graph = new HashMap<>();

        for (String nodeId : nodes.keySet()) {
            graph.put(nodeId, new ArrayList<>());
        }

        for (Connection connection : connections) {
            String connId = connection.getId();
            if (validateLink(connId)) {
                String[] split = connId.split("->");
                String source = split[0].split(":")[0];
                String target = split[1].split(":")[0];
                graph.get(source).add(target);
            }
        }

        List<String> checkedNodeStack = new ArrayList<>();
        List<String> currentPathStack = new ArrayList<>();

        for(String node: nodes.keySet()){
            if(!checkedNodeStack.contains(node)){
                reportCycle(node, graph, checkedNodeStack, currentPathStack, errs);
            }
        }
    }

    private boolean reportCycle(String node, Map<String, List<String>> graph,
                                List<String> checkedNodeStack, List<String> currentPathStack, List<String> errs){

        if (currentPathStack.contains(node)) {
            int startIdx = currentPathStack.indexOf(node);
            List<String> cyclePath = new ArrayList<>(currentPathStack.subList(startIdx, currentPathStack.size()));
            cyclePath.add(node);
            errs.add("Circular reference detected: " + String.join(" -> ", cyclePath));
            return true;
        }

        if (checkedNodeStack.contains(node)) {
            return false;
        }

        currentPathStack.add(node);

        boolean cycleFoundInAnyBranch = false;
        for (String nextNode : graph.get(node)) {
            if (reportCycle(nextNode, graph, checkedNodeStack, currentPathStack, errs)) {
                cycleFoundInAnyBranch = true;
            }
        }

        currentPathStack.remove(currentPathStack.size() - 1);

        checkedNodeStack.add(node);

        return cycleFoundInAnyBranch;
    }

    private boolean validateLink(String link){
        String[] split = link.split("->");

        String[] sourceParts = split[0].split(":");
        if (sourceParts.length < 1 || sourceParts[0].isBlank()) {
            return false;
        }
        String sourceNodeId = sourceParts[0];

        String[] targetParts = split[1].split(":");
        if (targetParts.length < 1 || targetParts[0].isBlank()) {
            return false;
        }
        String targetNodeId = targetParts[0];

        if (!nodes.containsKey(sourceNodeId)) {
            return false;
        }
        if (!nodes.containsKey(targetNodeId)) {
            return false;
        }

        return true;
    }
}
