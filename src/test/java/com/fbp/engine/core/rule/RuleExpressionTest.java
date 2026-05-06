package com.fbp.engine.core.rule;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;

import java.util.Map;

public class RuleExpressionTest {
    @Order(1)
    @Test
    @DisplayName("파싱 - 숫자 비교")
    void parseNumberComparison(){
        RuleExpression ruleExpression = RuleExpression.parse("temperature > 30.0");

        Message satisfied = new Message(Map.of("temperature", 31.0));
        Message unsatisfied = new Message(Map.of("temperature", 30.0));

        Assertions.assertAll(
                ()->Assertions.assertTrue(ruleExpression.evaluate(satisfied)),
                ()->Assertions.assertFalse(ruleExpression.evaluate(unsatisfied))
        );
    }

    @Order(2)
    @Test
    @DisplayName("파싱 문자열 비교")
    void parseStringComparison(){
        RuleExpression ruleExpression = RuleExpression.parse("status == \"ON\"");

        Message satisfied = new Message(Map.of("status", "ON"));
        Message unsatisfied = new Message(Map.of("status", "OFF"));

        Assertions.assertAll(
                ()->Assertions.assertTrue(ruleExpression.evaluate(satisfied)),
                ()->Assertions.assertFalse(ruleExpression.evaluate(unsatisfied))
        );
    }

    @Order(3)
    @Test
    @DisplayName("모든 연산자")
    void checkPortConfiguration(){
        RuleExpression greaterThan = RuleExpression.parse("temperature > 30.0");
        RuleExpression greaterThanOrEqualTo = RuleExpression.parse("temperature >= 30.0");
        RuleExpression lessThan = RuleExpression.parse("temperature < 30.0");
        RuleExpression lessThanOrEqualTo = RuleExpression.parse("temperature <= 30.0");
        RuleExpression equalTo = RuleExpression.parse("temperature == 30.0");
        RuleExpression notEqualTo = RuleExpression.parse("temperature != 30.0");

        Message upper = new Message(Map.of("temperature", 31.0));
        Message boundary = new Message(Map.of("temperature", 30.0));
        Message under = new Message(Map.of("temperature", 29.0));

        Assertions.assertAll(
                ()->Assertions.assertTrue(greaterThan.evaluate(upper)),
                ()->Assertions.assertFalse(greaterThan.evaluate(boundary)),
                ()->Assertions.assertFalse(greaterThan.evaluate(under)),

                ()->Assertions.assertTrue(greaterThanOrEqualTo.evaluate(upper)),
                ()->Assertions.assertTrue(greaterThanOrEqualTo.evaluate(boundary)),
                ()->Assertions.assertFalse(greaterThanOrEqualTo.evaluate(under)),

                ()->Assertions.assertFalse(lessThan.evaluate(upper)),
                ()->Assertions.assertFalse(lessThan.evaluate(boundary)),
                ()->Assertions.assertTrue(lessThan.evaluate(under)),

                ()->Assertions.assertFalse(lessThanOrEqualTo.evaluate(upper)),
                ()->Assertions.assertTrue(lessThanOrEqualTo.evaluate(boundary)),
                ()->Assertions.assertTrue(lessThanOrEqualTo.evaluate(under)),

                ()->Assertions.assertFalse(equalTo.evaluate(upper)),
                ()->Assertions.assertTrue(equalTo.evaluate(boundary)),
                ()->Assertions.assertFalse(equalTo.evaluate(under)),

                ()->Assertions.assertTrue(notEqualTo.evaluate(upper)),
                ()->Assertions.assertFalse(notEqualTo.evaluate(boundary)),
                ()->Assertions.assertTrue(notEqualTo.evaluate(under))
        );
    }

    @Order(4)
    @Test
    @DisplayName("잘못된 표현식")
    void checkIllegalFormat(){
        Assertions.assertThrows(IllegalArgumentException.class,
                ()->RuleExpression.parse("illegal"));

        Assertions.assertThrows(IllegalArgumentException.class,
                ()->RuleExpression.parse("i l l e g a l"));

        Assertions.assertThrows(IllegalArgumentException.class,
                ()->RuleExpression.parse("il ! legal"));

        Assertions.assertThrows(IllegalArgumentException.class,
                ()->RuleExpression.parse("il / legal"));
    }

    @Order(5)
    @Test
    @DisplayName("필드 없음")
    void notContainField(){
        RuleExpression ruleExpression = RuleExpression.parse("temperature > 30.0");

        Message notContainField = new Message(Map.of("key", 31.0));

        Assertions.assertFalse(ruleExpression.evaluate(notContainField));
    }


}
