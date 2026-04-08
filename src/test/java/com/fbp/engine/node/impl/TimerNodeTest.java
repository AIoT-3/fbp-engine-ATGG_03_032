package com.fbp.engine.node.impl;

import com.fbp.engine.core.connection.Connection;
import org.junit.jupiter.api.*;

public class TimerNodeTest {
    TimerNode testTargetNode;
    String testId;
    Connection testConn;

    @BeforeEach
    void setUp(){
        testId = "testId";
        testTargetNode = new TimerNode(testId, 500);
        testConn = new Connection("test");

        testTargetNode.getOutputPort("out").connect(testConn);
    }

    @Order(1)
    @Test
    @DisplayName("initialize 후 메시지 생성")
    void whenInitializeThanGenerateMessage(){
        testTargetNode.initialize();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Assertions.assertNotNull(testConn.poll());
    }

    @Order(2)
    @Test
    @DisplayName("tick 증가")
    void tickMustIncremented(){
        testTargetNode.initialize();

        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        for(int i=0; i<3; i++){
            Assertions.assertTrue(testConn.poll().toString().contains("tick="+i));
        }
    }

    @Order(3)
    @Test
    @DisplayName("shutdown 후 정지")
    void whenShutdownThenStop(){
        testTargetNode.initialize();

        try {
            Thread.sleep(550);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertNotEquals(0, testConn.getBufferSize());

        testTargetNode.shutdown();

        while (testConn.getBufferSize()!=0){testConn.poll();}

        try {
            Thread.sleep(2100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Assertions.assertEquals(0, testConn.getBufferSize());
    }

    @Order(4)
    @Test
    @DisplayName("주기 확인")
    void checkPeriod(){
        testTargetNode.initialize();

        try {
            Thread.sleep(2100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Assertions.assertEquals(4, testConn.getBufferSize());
    }


}
