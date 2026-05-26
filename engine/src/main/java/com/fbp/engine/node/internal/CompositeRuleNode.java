package com.fbp.engine.node.internal;

import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.core.rule.RuleExpression;
import com.fbp.engine.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class CompositeRuleNode extends AbstractNode {
    public enum Operator{
        AND, OR
    }

    private final List<Predicate<Message>> conditions = new ArrayList<>();
    private final Operator operator;

    public CompositeRuleNode(String id, Operator operator) {
        super(id);
        if(operator==null){
            throw new IllegalArgumentException("operator must be notNull");
        }
        this.operator = operator;

        addInputPort("in");
        addOutputPort("match");
        addOutputPort("mismatch");
    }

    public void addCondition(Predicate<Message> condition){
        if(condition == null){
            throw new IllegalArgumentException("condition must be notNull");
        }
        conditions.add(condition);
    }

    public void addCondition(String field, String op, Object value){
        if(field == null || field.isBlank()){
            throw new IllegalArgumentException("field must be notBlank");
        }

        if( ! (op.equals(">") ||
                op.equals(">=") ||
                op.equals("<") ||
                op.equals("<=") ||
                op.equals("==") ||
                op.equals("!="))){
            throw new IllegalArgumentException("not supported operator... " + op);
        }

        if( ! (value instanceof Number) ){
            throw new IllegalArgumentException("value must be instance of Number");
        }

        conditions.add(new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression ruleExpression = new RuleExpression(field, op, value);
                return ruleExpression.evaluate(message);
            }
        });
    }

    @Override
    public void onProcess(String portName, Message message) {
        if(message==null){
            return;
        }

        if(conditions.isEmpty()){
            if(operator.equals(Operator.AND)){
                send("match", message);
            }

            if(operator.equals(Operator.OR)){
                send("mismatch", message);
            }
        }

        boolean isAMatched = false;
        boolean isANotMatched = false;
        for (Predicate<Message> condition : conditions) {
            if(condition.test(message)){
                isAMatched = true;
            }else{
                isANotMatched = true;
            }
        }

        if(operator.equals(Operator.AND)){
            if( ! isANotMatched){
                send("match", message);
            }else{
                send("mismatch", message);
            }
        }

        if(operator.equals(Operator.OR)){
            if(isAMatched){
                send("match", message);
            }else{
                send("mismatch", message);
            }
        }
    }
}
