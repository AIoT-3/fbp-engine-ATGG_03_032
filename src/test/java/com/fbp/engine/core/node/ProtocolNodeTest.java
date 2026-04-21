package com.fbp.engine.core.node;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProtocolNodeTest {
    Map<String, Object> config = Map.of("test", "value", "test2", "value2");

    @Spy
    ProtocolNode protocolNode = new ProtocolNode("protocol", config) {
        @Override
        protected void connect() throws Exception {}
        @Override
        protected void disconnect() {}
        @Override
        public void onProcess(String portName, Message message) {}
    };

    @Order(1)
    @Test
    @DisplayName("초기 상태")
    void initializedState(){
        Assertions.assertEquals(ProtocolNode.ConnectionState.DISCONNECTED, protocolNode.getConnectionState());
    }

    @Order(2)
    @Test
    @DisplayName("config 조회")
    void getConfig(){
        Assertions.assertAll(
                ()->Assertions.assertEquals(config.get("test"), protocolNode.getConfig("test")),
                ()->Assertions.assertEquals(config.get("test2"), protocolNode.getConfig("test2"))
        );
    }

    @Order(3)
    @Test
    @DisplayName("initialize -> CONNECTED")
    void initializeToConnected() throws Exception {
        protocolNode.initialize();

        Assertions.assertEquals(ProtocolNode.ConnectionState.CONNECTED, protocolNode.getConnectionState());
    }

    @Order(4)
    @Test
    @DisplayName("initialize -> 연결 실패 시 상태")
    void initializeToConnectFailed() throws Exception {
        doThrow(Exception.class).when(protocolNode).connect();

        protocolNode.initialize();

        Assertions.assertEquals(ProtocolNode.ConnectionState.ERROR, protocolNode.getConnectionState());
    }

    @Order(5)
    @Test
    @DisplayName("shutdown → DISCONNECTED")
    void ifShutdownThenDisconnected(){
        protocolNode.shutdown();

        Assertions.assertEquals(ProtocolNode.ConnectionState.DISCONNECTED, protocolNode.getConnectionState());
    }

    @Order(6)
    @Test
    @DisplayName("isConnected 반환갑")
    void checkIsConnectedReturnValue(){
        Assertions.assertFalse(protocolNode.isConnected());

        protocolNode.initialize();

        Assertions.assertTrue(protocolNode.isConnected());
    }

    @Order(7)
    @Test
    @DisplayName("재연결 시도")
    void checkReconnectTrial() throws Exception {
        doThrow(Exception.class).when(protocolNode).connect();

        protocolNode.initialize();

        verify(protocolNode, times(1)).reconnect();
    }


}
