package com.fbp.engine.node.internal;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.core.rule.RuleExpression;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.function.Predicate;

public class TimeWindowRuleNodeTest {
    TimeWindowRuleNode timeWindowRuleNode;

    Connection passConn;
    Connection alertConn;

    Message conditionSatisfied;

    @BeforeEach
    void setUp(){
        timeWindowRuleNode = new TimeWindowRuleNode("time-ruler", new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression parse = RuleExpression.parse("temperature >= 35.0");
                return parse.evaluate(message);
            }
        }, 50, 3);

        passConn = new Connection("passConn");
        alertConn = new Connection("alertConn");

        timeWindowRuleNode.getOutputPort("pass").connect(passConn);
        timeWindowRuleNode.getOutputPort("alert").connect(alertConn);

        conditionSatisfied = new Message(Map.of("temperature", 35.0));
    }

    @Order(1)
    @Test
    @DisplayName("기준 미달 -> pass")
    void checkUnsatisfied(){
        timeWindowRuleNode.process("in", conditionSatisfied);
        Assertions.assertNotNull(passConn.poll());

        timeWindowRuleNode.process("in", conditionSatisfied);
        Assertions.assertNotNull(passConn.poll());

    }

    @Order(2)
    @Test
    @DisplayName("기준 도달 -> alert")
    void checkSatisfied(){
        timeWindowRuleNode.process("in", conditionSatisfied);
        Assertions.assertNotNull(passConn.poll());

        timeWindowRuleNode.process("in", conditionSatisfied);
        Assertions.assertNotNull(passConn.poll());

        timeWindowRuleNode.process("in", conditionSatisfied);
        Assertions.assertNotNull(alertConn.poll());
    }

    @Order(3)
    @Test
    @DisplayName("시간 창 만료")
    void timeWindowExpiration() throws InterruptedException {
        timeWindowRuleNode.process("in", conditionSatisfied);
        Assertions.assertNotNull(passConn.poll());

        timeWindowRuleNode.process("in", conditionSatisfied);
        Assertions.assertNotNull(passConn.poll());

        Thread.sleep(50);

        timeWindowRuleNode.process("in", conditionSatisfied);
        Assertions.assertNotNull(passConn.poll());
    }

    @Order(4)
    @Test
    @DisplayName("조건 불만족 메시지")
    void checkConditionUnsatisfiedMessage(){
        Message unsatisfied = new Message(Map.of("temperature", 30.0));

        timeWindowRuleNode.process("in", unsatisfied);
        timeWindowRuleNode.process("in", unsatisfied);
        timeWindowRuleNode.process("in", unsatisfied);

        Assertions.assertEquals(0, timeWindowRuleNode.getEvents().size());
    }
}
