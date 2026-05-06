package com.fbp.engine.node.internal;

import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.message.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Predicate;

@Slf4j
public class RuleNode extends AbstractNode {
    private Predicate<Message> condition;

    public RuleNode(String id, Predicate<Message> condition) {
        super(id);
        if(condition == null){
            throw new IllegalArgumentException("condition must be notNull");
        }
        this.condition = condition;

        addInputPort("in");
        addOutputPort("match");
        addOutputPort("mismatch");
    }

    @Override
    public void onProcess(String portName, Message message) {
        if(message == null){
            return;
        }


        if(condition.test(message)){
            send("match", message);
        }else{
            send("mismatch", message);
        }
    }
}
