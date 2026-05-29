package com.fbp.engine.node.routing;

import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DynamicRouterNode extends AbstractNode {

    private final List<RoutingRule> rules = new ArrayList<>();
    private final String defaultPort;

    /**
     * Creates a DynamicRouterNode.
     * @param id The node ID
     * @param defaultPort The port to send messages to if no rules match. If null, a default "default" port is used.
     */
    public DynamicRouterNode(String id, String defaultPort) {
        super(id);
        this.defaultPort = defaultPort != null && !defaultPort.isBlank() ? defaultPort : "default";
        
        addInputPort("in");
        addOutputPort(this.defaultPort);
    }

    public void addRule(RoutingRule rule) {
        rules.add(rule);
        
        if (getOutputPort(rule.getPort()) == null) {
            addOutputPort(rule.getPort());
        }
    }
    
    public void removeRule(RoutingRule rule) {
        rules.remove(rule);
    }
    
    public void clearRules() {
        rules.clear();
    }

    @Override
    public void onProcess(String portName, Message message) {
        if (!"in".equals(portName)) {
            log.warn("[{}] Received message on unknown port: {}", getId(), portName);
            return;
        }

        for (RoutingRule rule : rules) {
            if (rule.matches(message)) {
                log.debug("[{}] Message matched rule for field '{}', routing to port '{}'", getId(), rule.getField(), rule.getPort());
                send(rule.getPort(), message);
                return;
            }
        }

        log.debug("[{}] No rules matched, routing to default port '{}'", getId(), defaultPort);
        send(defaultPort, message);
    }
}
