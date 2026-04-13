package com.fbp.engine.node.impl;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class CollectorNodeTest {
    CollectorNode target;
    GeneratorNode generatorNode;
    Message message;

    @BeforeEach
    void setUp(){
        target = new CollectorNode("test-target");
        generatorNode = new GeneratorNode("generator");
        message = new Message(Map.of("test","value"));
    }

    @Order(1)
    @Test
    @DisplayName("메시지 수집")
    void messageCollect(){
        target.process("in",  message);

        Assertions.assertEquals(message, target.getCollected().getFirst());
    }

    @Order(2)
    @Test
    @DisplayName("수집 순서 보존")
    void checkOrderCollectedMessage(){
        List<Message> messages = new ArrayList<>();
        for(int i=0; i<5; i++){
            Message m = new Message(Map.of("test"+i,"value"+i));
            target.process("in", m);
            messages.add(m);
        }

        Assertions.assertEquals(messages, target.getCollected());
    }

    @Order(3)
    @Test
    @DisplayName("초기 상태 빈 리스트")
    void checkInitializedState(){
        Assertions.assertEquals(Collections.EMPTY_LIST, target.getCollected());
    }

    @Order(4)
    @Test
    @DisplayName("InputPort 존재")
    void checkInputPort(){
        Assertions.assertNotNull(target.getInputPort("in"));
    }

    @Order(5)
    @Test
    @DisplayName("파이프라인 연결 검증")
    void checkPipeLineGeneratorNodeToCollectorNode(){
        Connection connection = new Connection("get-to-col");

        generatorNode.getOutputPort("out").connect(connection);
        connection.setTarget(target.getInputPort("in"));

        for(int i=0; i<5; i++){
            generatorNode.generate("test", 5);
        }

        while(connection.getBufferSize()!=0){
            connection.poll();
        }

        Assertions.assertEquals(5, target.getCollected().size());

    }
}
