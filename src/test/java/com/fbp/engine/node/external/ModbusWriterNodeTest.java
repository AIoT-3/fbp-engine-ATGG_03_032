package com.fbp.engine.node.external;

import com.fbp.engine.message.Message;
import com.fbp.engine.protocol.modbus.ModbusTcpSimulator;
import org.junit.jupiter.api.*;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ModbusWriterNodeTest {

    static Map<String, Object> baseConfig = Map.of(
            "host", "localhost",
            "slaveId", 1,
            "registerAddress", 2,
            "valueField", "temperature"
    );

    static ModbusTcpSimulator simulator;
    static ModbusWriterNode node;

    @BeforeAll
    static void startSimulator() {
        simulator = new ModbusTcpSimulator(5020, 10);
        simulator.start();
    }

    @BeforeEach
    void setUpNode() {
        node = new ModbusWriterNode("writer", Map.of(
                "host", "localhost",
                "port", 5020,
                "slaveId", 1,
                "registerAddress", 2,
                "valueField", "temperature"
        ));
    }

    @AfterEach
    void tearDownNode() {
        node.shutdown();
    }

    @AfterAll
    static void stopSimulator() {
        simulator.stop();
    }

    @Test
    @Order(1)
    @DisplayName("포트 구성 — in 포트 존재")
    void portConfiguration() {
        ModbusWriterNode target = new ModbusWriterNode("writer", baseConfig);
        assertNotNull(target.getInputPort("in"));
    }

    @Test
    @Order(2)
    @DisplayName("초기 상태 — 생성 직후 isConnected() false")
    void initialState() {
        ModbusWriterNode target = new ModbusWriterNode("writer", baseConfig);
        assertFalse(target.isConnected());
    }

    @Test
    @Order(3)
    @DisplayName("config 확인 — registerAddress, valueField 설정값 일치")
    void configValues() {
        ModbusWriterNode target = new ModbusWriterNode("writer", baseConfig);
        assertEquals(2, target.getConfig("registerAddress"));
        assertEquals("temperature", target.getConfig("valueField"));
    }

    @Test
    @Order(4)
    @DisplayName("연결 성공 — initialize() 후 isConnected() true")
    void connectSuccess() {
        node.initialize();
        assertTrue(node.isConnected());
    }

    @Test
    @Order(5)
    @DisplayName("레지스터 쓰기 — process() 후 시뮬레이터 getRegister() 값 변경 확인")
    void writeRegister() {
        node.initialize();

        Message message = new Message(Map.of("temperature", 100));
        node.onProcess("in", message);

        assertEquals(100, simulator.getRegister(2));
    }

    @Test
    @Order(6)
    @DisplayName("스케일 변환 — scale=10.0, 25.5 → 255로 변환되어 기록")
    void scaleConversion() {
        ModbusWriterNode scaledNode = new ModbusWriterNode("writer-scaled", Map.of(
                "host", "localhost",
                "port", 5020,
                "slaveId", 1,
                "registerAddress", 3,
                "valueField", "temperature",
                "scale", 10.0
        ));
        scaledNode.initialize();

        Message message = new Message(Map.of("temperature", 25.5));
        scaledNode.onProcess("in", message);

        assertEquals(255, simulator.getRegister(3));

        scaledNode.shutdown();
    }

    @Test
    @Order(7)
    @DisplayName("shutdown 후 연결 해제 — isConnected() false")
    void shutdownDisconnects() {
        node.initialize();
        assertTrue(node.isConnected());

        node.shutdown();
        assertFalse(node.isConnected());
    }
}