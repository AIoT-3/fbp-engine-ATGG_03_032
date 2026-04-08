package com.fbp.engine.node.impl;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class FilterNodeTest {
    @Mock
    Connection connection;
    FilterNode filterNode;

    Connection realConn;
    FilterNode filterNode2;

    @BeforeEach
    void setUp() {
        filterNode = new FilterNode("filter-1", "temperature", 30.0);
        filterNode.getOutputPort("out").connect(connection);

        realConn = new Connection("real-conn");
        filterNode2 = new FilterNode("filter-1", "temperature", 30.0);
        filterNode2.getOutputPort("out").connect(realConn);
    }

    @Order(1)
    @Test
    @DisplayName("조건 만족 시 통과")
    void passWhenConditionMet() {
        Message msg = new Message(Map.of("temperature", 35.0));
        filterNode.process(msg);
        verify(connection, times(1)).deliver(msg);
    }

    @Order(2)
    @Test
    @DisplayName("조건 미달 시 차단")
    void blockWhenConditionNotMet() {
        Message msg = new Message(Map.of("temperature", 25.0));
        filterNode.process(msg);
        verify(connection, never()).deliver(any());
    }

    @Order(3)
    @Test
    @DisplayName("경계값 처리")
    void passOnBoundaryValue() {
        Message msg = new Message(Map.of("temperature", 30.0));
        filterNode.process(msg);
        verify(connection, times(1)).deliver(msg);
    }

    @Order(4)
    @Test
    @DisplayName("키 없는 메시지")
    void handleMessageWithoutKeySafely() {
        Message msg = new Message(Map.of("humidity", 50.0));
        Assertions.assertDoesNotThrow(() -> filterNode.process(msg));
        verify(connection, never()).deliver(any());
    }

    /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// /// ///
    @Order(1)
    @Test
    @DisplayName("조건 만족 -> send 호출")
    void whenConditionSatisfiedThenCallSend(){
        filterNode2.process(new Message(Map.of("temperature", 35.0)));

        Assertions.assertNotNull(realConn.poll());
    }

    @Order(2)
    @Test
    @DisplayName("조건 미달 -> 차단")
    void whenConditionUnsatisfiedThenBlock(){
        filterNode.process(new Message(Map.of("temperature", 29.9)));

        Assertions.assertNull(realConn.poll());
    }

    @Order(3)
    @Test
    @DisplayName("포트 구성 확인")
    void checkPortConfiguration(){
        Assertions.assertAll(
                () -> Assertions.assertNotNull(filterNode.getOutputPort("out")),
                () -> Assertions.assertNotNull(filterNode.getInputPort("in"))
        );
    }
}
