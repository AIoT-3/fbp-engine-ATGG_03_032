package com.fbp.engine.core.rule;

import com.fbp.engine.message.Message;
import java.util.Objects;

public class RuleExpression {
    private String field;
    private String operator;
    private Object value;

    public RuleExpression(String field, String operator, Object value) {
        if(field == null || field.isBlank()){
            throw new IllegalArgumentException("field must be notBlank");
        }
        if(operator == null || operator.isBlank()){
            throw new IllegalArgumentException("operator must be notBlank");
        }
        if(value ==null){
            throw new IllegalArgumentException("value must be notNull");
        }

        this.field = field;
        this.operator = operator;
        this.value = value;
    }

    public static RuleExpression parse(String expression){
        String[] parts = expression.trim().split("\\s+");

        String field=null;
        String operator=null;

        if(parts.length != 3){
            throw new IllegalArgumentException();
        }

        if(  ! (parts[1].equals(">") ||
                parts[1].equals(">=") ||
                parts[1].equals("<") ||
                parts[1].equals("<=") ||
                parts[1].equals("==") ||
        parts[1].equals("!="))){
            throw new IllegalArgumentException("not supported operator... but, " + parts[1]);
        }

        field = parts[0].trim();
        operator = parts[1].trim();

        try{
            Double value = Double.parseDouble(parts[2].trim());

            return new RuleExpression(field, operator, value);
        } catch (NumberFormatException e) {
            String value = parts[2].trim();

            if(value.startsWith("\"") && value.endsWith("\"")){
                return new RuleExpression(field, operator, value.replace("\"", ""));
            }else{
                throw new IllegalArgumentException("value must be a number or a quoted string (e.g. \"ON\")");
            }
        }
    }

    public boolean evaluate(Message message){
        if(message == null){
            throw new IllegalArgumentException("message must be notNull");
        }

        Object fieldValue = message.get(field);
        if(fieldValue == null){
            return false;
        }

        if(fieldValue instanceof Number){
            return evaluateNumber((Number) fieldValue);
        }
        if(fieldValue instanceof String){
            return evaluateString((String) fieldValue);
        }

        return false;
    }

    private boolean evaluateNumber(Number evaluateTarget){
        Double dobEvaluateTarget = evaluateTarget.doubleValue();

        Double compareValue = null;

        if(value instanceof Number){
            compareValue = ((Number) value).doubleValue();
        }else{
            return false;
        }

        if(operator.equals(">")){
            return dobEvaluateTarget > compareValue;
        }else if (operator.equals(">=")){
            return dobEvaluateTarget >= compareValue;
        }else if(operator.equals("<")){
            return dobEvaluateTarget < compareValue;
        }else if (operator.equals("<=")){
            return dobEvaluateTarget <= compareValue;
        }else if (operator.equals("==")) {
            return Math.abs(dobEvaluateTarget - compareValue) < 1e-9;
        } else if (operator.equals("!=")) {
            return Math.abs(dobEvaluateTarget - compareValue) >= 1e-9;
        }
        return false;
    }

    private boolean evaluateString(String evaluateTarget){
        String compareValue = null;

        if(value instanceof String){
            compareValue = (String) value;
        }else{
            return false;
        }

        if(operator.equals(">")){
            return evaluateTarget.length() > compareValue.length();
        }else if (operator.equals(">=")){
            return evaluateTarget.length() >= compareValue.length();
        }else if(operator.equals("<")){
            return evaluateTarget.length() < compareValue.length();
        }else if (operator.equals("<=")){
            return evaluateTarget.length() <= compareValue.length();
        }else if (operator.equals("==")){
            return Objects.equals(evaluateTarget, compareValue);
        }else if(operator.equals("!=")) {
            return !Objects.equals(evaluateTarget, compareValue);
        }
        return false;
    }
}
