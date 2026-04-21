package com.fbp.engine.node.internal;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ThresholdFilterNodeTest {
    @Spy
    ThresholdFilterNode target = new ThresholdFilterNode("test-filter", "temperature", 30.0);

    Connection alertConn;
    Connection normalConn;

    @BeforeEach
    void setUp() {
        alertConn = new Connection("alert-link");
        normalConn = new Connection("normal-link");

        target.getOutputPort("alert").connect(alertConn);
        target.getOutputPort("normal").connect(normalConn);
    }

    @Test
    @Order(1)
    @DisplayName("초과 시 alert 포트로 전송 확인")
    void filterAlertValue() {
        Message msg = new Message(Map.of("temperature", 31.5));

        target.onProcess("in", msg);

        assertNotNull(alertConn.poll(), "31.5는 30.0 초과이므로 alert 포트로 나가야 합니다.");
    }

    @Test
    @Order(2)
    @DisplayName("이하 시 normal 포트로 전송 확인")
    void filterNormalValue() {
        Message msg = new Message(Map.of("temperature", 25.0));

        target.onProcess("in", msg);

        assertNotNull(normalConn.poll(), "25.0은 30.0 이하이므로 normal 포트로 나가야 합니다.");
    }

    @Test
    @Order(3)
    @DisplayName("경계값(정확히 같은 값)은 normal로 분기")
    void filterBoundaryValue() {
        Message msg = new Message(Map.of("temperature", 30.0));

        target.onProcess("in", msg);

        assertNotNull(normalConn.poll(), "임계값과 같은 값은 '초과'가 아니므로 normal로 가야 합니다.");
    }

    @Test
    @Order(4)
    @DisplayName("키가 없는 메시지 수신 시 무시 처리")
    void handleMissingKey() {
        Message msg = new Message(Map.of("humidity", 50.0));

        assertDoesNotThrow(() -> target.onProcess("in", msg));

        assertTrue(alertConn.getBufferSize()==0);
        assertTrue(normalConn.getBufferSize()==0);
    }

    @Test
    @Order(5)
    @DisplayName("양쪽 동시 검증 (CollectorNode 역할 시뮬레이션)")
    void verifyBothPortsWithMultipleMessages() {
        Message highMsg = new Message(Map.of("temperature", 45.0));
        Message lowMsg = new Message(Map.of("temperature", 10.0));

        target.onProcess("in", highMsg);
        target.onProcess("in", lowMsg);
        target.onProcess("in", new Message(Map.of("temperature", 50.0)));

        int alertCount = 0;
        while(alertConn.getBufferSize() != 0) {
            if(alertConn.poll() != null) {
                alertCount++;
            }
        }
        int normalCount = 0;
        while(normalConn.getBufferSize() != 0) {
            if(normalConn.poll() != null) {
                normalCount++;
            }
        }
        assertEquals(2, alertCount, "Alert 포트로 2개의 메시지가 전달되어야 합니다.");
        assertEquals(1, normalCount, "Normal 포트로 1개의 메시지가 전달되어야 합니다.");
    }
}