package com.fbp.engine.node.internal;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.core.rule.RuleExpression;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.function.Predicate;

public class CompositeRuleNodeTest {
    CompositeRuleNode andRuleNode;
    CompositeRuleNode orRuleNode;

    Connection matchConn;
    Connection mismatchConn;

    @BeforeEach
    void setUp(){
        andRuleNode = new CompositeRuleNode("and-rule", CompositeRuleNode.Operator.AND);
        orRuleNode = new CompositeRuleNode("or-rule", CompositeRuleNode.Operator.OR);

        matchConn = new Connection("match");
        mismatchConn = new Connection("mismatch");

        andRuleNode.getOutputPort("match").connect(matchConn);
        andRuleNode.getOutputPort("mismatch").connect(mismatchConn);

        orRuleNode.getOutputPort("match").connect(matchConn);
        orRuleNode.getOutputPort("mismatch").connect(mismatchConn);
    }

    @Order(1)
    @Test
    @DisplayName("AND - 모두 만족")
    void checkAndRuleAllSatisfied(){
        andRuleNode.addCondition(new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression ruleExpression = RuleExpression.parse("temperature >= 30");
                return ruleExpression.evaluate(message);
            }
        });

        andRuleNode.addCondition(new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression ruleExpression = RuleExpression.parse("status == \"ON\"");
                return ruleExpression.evaluate(message);
            }
        });

        Message allSatisfied = new Message(Map.of("temperature", 31.0,
                "status", "ON"));

        Message oneSatisfied = new Message(Map.of("temperature", 31.0));

        andRuleNode.process("in", allSatisfied);
        Assertions.assertNotNull(matchConn.poll());

        andRuleNode.process("in", oneSatisfied);
        Assertions.assertNotNull(mismatchConn.poll());
    }

    @Order(2)
    @Test
    void checkAndRuleOneSatisfied(){
        andRuleNode.addCondition(new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression ruleExpression = RuleExpression.parse("temperature >= 30");
                return ruleExpression.evaluate(message);
            }
        });

        andRuleNode.addCondition(new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression ruleExpression = RuleExpression.parse("status == \"ON\"");
                return ruleExpression.evaluate(message);
            }
        });

        Message oneSatisfied = new Message(Map.of("status", "ON",
                "temperature", -10.0));

        andRuleNode.process("in", oneSatisfied);

        Assertions.assertNotNull(mismatchConn.poll());
    }

    @Order(3)
    @Test
    @DisplayName("OR - 하나 만족")
    void checkOrRuleOneSatisfied(){
        orRuleNode.addCondition(new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression ruleExpression = RuleExpression.parse("temperature >= 30");
                return ruleExpression.evaluate(message);
            }
        });

        orRuleNode.addCondition(new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression ruleExpression = RuleExpression.parse("status == \"ON\"");
                return ruleExpression.evaluate(message);
            }
        });

        Message oneSatisfied = new Message(Map.of("temperature", 31.0));
        Message oneSatisfied1 = new Message(Map.of("status", "ON"));

        orRuleNode.process("in", oneSatisfied);
        Assertions.assertNotNull(matchConn.poll());

        orRuleNode.process("in", oneSatisfied1);
        Assertions.assertNotNull(matchConn.poll());
    }

    @Order(4)
    @Test
    @DisplayName("OR - 모두 불만족")
    void checkOrRuleAllUnsatisfied(){
        orRuleNode.addCondition(new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression ruleExpression = RuleExpression.parse("temperature >= 30");
                return ruleExpression.evaluate(message);
            }
        });

        orRuleNode.addCondition(new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression ruleExpression = RuleExpression.parse("status == \"ON\"");
                return ruleExpression.evaluate(message);
            }
        });

        Message message = new Message(Map.of("key","value"));

        orRuleNode.process("in", message);

        Assertions.assertNotNull(mismatchConn.poll());
    }

    @Order(5)
    @Test
    @DisplayName("빈 조건")
    void checkEmptyConditionCase(){
        Message message = new Message(Map.of("key", "value"));

        andRuleNode.process("in", message);
        Assertions.assertNotNull(matchConn.poll());

        orRuleNode.process("in", message);
        Assertions.assertNotNull(mismatchConn.poll());
    }
}
