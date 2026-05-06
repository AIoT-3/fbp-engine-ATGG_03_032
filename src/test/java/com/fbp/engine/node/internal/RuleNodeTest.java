package com.fbp.engine.node.internal;

import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.core.rule.RuleExpression;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;

import java.util.Map;
import java.util.function.Predicate;

public class RuleNodeTest {
    RuleNode ruleNode;

    FlowEngine flowEngine;
    Flow flow;
    CollectorNode matchCollector;
    CollectorNode mismatchCollector;


    @BeforeEach
    void setUp(){
        ruleNode = new RuleNode("ruler", new Predicate<Message>() {
            @Override
            public boolean test(Message message) {
                RuleExpression ruleExpression = new RuleExpression("temperature", ">", 35.0);
                return ruleExpression.evaluate(message);
            }
        });

        matchCollector = new CollectorNode("matched");
        mismatchCollector = new CollectorNode("mismatched");

        flow = new Flow("flow");
        flow.addNode(ruleNode)
                .addNode(matchCollector)
                .addNode(mismatchCollector)
                .connect(ruleNode.getId(), "match", matchCollector.getId(), "in")
                .connect(ruleNode.getId(), "mismatch", mismatchCollector.getId(), "in");

        flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.startFlow("flow");
    }

    @AfterEach
    void end(){
        flowEngine.shutdown();
    }


    @Order(1)
    @Test
    @DisplayName("조건 만족 -> match")
    void ifConditionSatisfiedThenSendMatchPort() throws InterruptedException {
        ruleNode.process("in", new Message(Map.of("temperature", 36.0)));

        Thread.sleep(50);

        Assertions.assertAll(
                ()->Assertions.assertEquals(1, matchCollector.getCollected().size()),
                ()->Assertions.assertEquals(0, mismatchCollector.getCollected().size())
        );
    }

    @Order(2)
    @Test
    @DisplayName("조건 불만족 -> mismatch")
    void ifConditionUnsatisfiedThenSendMismatchPort() throws InterruptedException {
        ruleNode.process("in", new Message(Map.of("temperature", 25.0)));

        Thread.sleep(50);

        Assertions.assertAll(
                ()->Assertions.assertEquals(0, matchCollector.getCollected().size()),
                ()->Assertions.assertEquals(1, mismatchCollector.getCollected().size())
        );
    }

    @Order(3)
    @Test
    @DisplayName("포트 구성")
    void checkPortConfiguration(){
        Assertions.assertAll(
                ()->Assertions.assertNotNull(ruleNode.getInputPort("in")),
                ()->Assertions.assertNotNull(ruleNode.getOutputPort("match")),
                ()->Assertions.assertNotNull(ruleNode.getOutputPort("mismatch"))
        );
    }

    @Order(4)
    @Test
    @DisplayName("null 필드 처리")
    void processNullField(){
        Assertions.assertDoesNotThrow(
                ()->{
                    ruleNode.process("in", new Message(Map.of("unknownKey", "unknownValue")));
                });
    }

    @Order(5)
    @Test
    @DisplayName("다수 메시지 분기")
    void branchOutMixedMessage() throws InterruptedException {
        ruleNode.process("in", new Message(Map.of(
                "temperature", 36.0,
                "whatKey", "whatValue")));

        ruleNode.process("in", new Message(Map.of(
                "whatKey2", "whatValue2",
                "whatKey3", "whatValue3"
        )));

        ruleNode.process("in", new Message(Map.of(
                "temperature", 25.0,
                "whatKey", "whatValue")));

        Thread.sleep(20);

        Assertions.assertAll(
                ()->Assertions.assertEquals(2, mismatchCollector.getCollected().size()),
                ()->Assertions.assertEquals(1, matchCollector.getCollected().size())
        );
    }
}
