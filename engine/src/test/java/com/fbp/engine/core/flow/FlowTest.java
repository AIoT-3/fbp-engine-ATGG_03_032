package com.fbp.engine.core.flow;

import com.fbp.engine.message.Message;
import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.node.internal.*;
import org.junit.jupiter.api.*;

import static org.mockito.Mockito.*;

public class FlowTest {
    Flow target;

    @BeforeEach
    void setUp(){
        target = new Flow("test");
    }

    @Order(1)
    @Test
    @DisplayName("노드 등록")
    void addNode(){
        AbstractNode node = new AbstractNode("node-module") {
            @Override
            public void onProcess(String portName, Message message) {

            }
        };

        target.addNode(node);

        Assertions.assertTrue(target.getNodes().contains(node));
    }

    @Order(2)
    @Test
    @DisplayName("메서드 체이닝")
    void methodChaining(){
        FilterNode node = new FilterNode("test-module", "key", 3);

        Assertions.assertDoesNotThrow(()->{
            target.addNode(node)
                    .connect(node.getId(),"out", node.getId(),"in");
        });
    }

    @Order(3)
    @Test
    @DisplayName("정상 연결")
    void connectSuccessfully(){
        FilterNode node = new FilterNode("test-module", "key", 3);
        target.addNode(node)
                .connect(node.getId(),"out", node.getId(),"in");

        Assertions.assertEquals(1, target.getConnections().size());
    }

    @Order(4)
    @Test
    @DisplayName("존재하지 않는 소스 노드 ID")
    void notExistsSourceNodeId(){
        FilterNode node = new FilterNode("test-module", "key", 3);
        target.addNode(node);
        Assertions.assertThrows(IllegalArgumentException.class, () ->{
            target.connect("unknown", "sp", "test-module", "in");
        });
    }

    @Order(5)
    @Test
    @DisplayName("존재하지 않는 대상 노드 Id")
    void notExistsTargetNodeId(){
        FilterNode node = new FilterNode("test-module", "key", 3);
        target.addNode(node);
        Assertions.assertThrows(IllegalArgumentException.class, () ->{
            target.connect("test-module", "out", "unknown", "in");
        });
    }

    @Order(6)
    @Test
    @DisplayName("존재하지 않는 소스 포트")
    void notExistsSourcePort(){
        FilterNode node = new FilterNode("test-module", "key", 3);
        target.addNode(node);
        Assertions.assertThrows(IllegalArgumentException.class, () ->{
            target.connect("test-module", "unknown", "test-module", "in");
        });
    }

    @Order(7)
    @Test
    @DisplayName("존재하지 않는 대상 포트")
    void notExistsTargetPort(){
        FilterNode node = new FilterNode("test-module", "key", 3);
        target.addNode(node);
        Assertions.assertThrows(IllegalArgumentException.class, () ->{
            target.connect("test-module", "out", "test-module", "unknown");
        });
    }

    @Order(8)
    @Test
    @DisplayName("validate - 빈 Flow")
    void validateEmptyFlow(){
        Assertions.assertFalse(target.validate().isEmpty());
    }

    @Order(9)
    @Test
    @DisplayName("validate - 정상 Flow")
    void validateNormalFlow(){
        FilterNode node = new FilterNode("test-module", "key", 3);
        target.addNode(node);
        Assertions.assertTrue(target.validate().isEmpty());
    }

    @Order(10)
    @Test
    @DisplayName("initialize - 전체 호출")
    void initializeCallAll(){
        FilterNode filterNode = spy(new FilterNode("f","f",0));
        PrintNode printNode = spy(new PrintNode("p"));
        GeneratorNode generatorNode = spy(new GeneratorNode("g"));

        target.addNode(filterNode)
                .addNode(printNode)
                .addNode(generatorNode);

        target.initialize();

        verify(filterNode,times(1)).initialize();
        verify(printNode,times(1)).initialize();
        verify(generatorNode, times(1)).initialize();
    }

    @Order(11)
    @Test
    @DisplayName("shutdown - 전체 호출")
    void shutdownCallAll(){
        TimerNode timerNode = spy(new TimerNode("t",500));
        FilterNode filterNode = spy(new FilterNode("f","f",0));
        PrintNode printNode = spy(new PrintNode("p"));
        GeneratorNode generatorNode = spy(new GeneratorNode("g"));

        target.addNode(timerNode).
                addNode(filterNode)
                .addNode(printNode)
                .addNode(generatorNode);

        target.shutdown();

        verify(timerNode,times(1)).shutdown();
        verify(filterNode,times(1)).shutdown();
        verify(printNode,times(1)).shutdown();
        verify(generatorNode, times(1)).shutdown();
    }

    @Order(12)
    @Test
    @DisplayName("순환 참조 탐지")
    void checkCircularReference(){
        LogNode logNode = new LogNode("l1");
        LogNode logNode1 = new LogNode("l2");
        LogNode logNode2 = new LogNode("l3");
        LogNode logNode3 = new LogNode("l4");
        LogNode logNode4 = new LogNode("l5");
        LogNode logNode5 = new LogNode("l6");

        target.addNode(logNode)
                .addNode(logNode1)
                .addNode(logNode2)
                .addNode(logNode3)
                .addNode(logNode4)
                .addNode(logNode5)
                .connect("l1","out","l2","in")
                .connect("l2", "out", "l3", "in")
                .connect("l3", "out", "l4", "in")
                .connect("l4", "out", "l5", "in")
                .connect("l5", "out", "l6", "in")
                .connect("l5", "out", "l1", "in")
                .connect("l6","out", "l3","in")
                .connect("l6", "out", "l6","in");

        Assertions.assertFalse(target.validate().isEmpty());
    }
}
