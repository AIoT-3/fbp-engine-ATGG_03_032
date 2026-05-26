package com.fbp.engine.protocol.modbus.exception;

import org.junit.jupiter.api.*;

public class ModbusExceptionTest {
    ModbusException modbusException;

    @BeforeEach
    void setUp(){
        modbusException = new ModbusException(3, 1);
    }

    @Order(1)
    @Test
    @DisplayName("getMessage 포맷")
    void checkGetMessageFormat(){
        Assertions.assertTrue(modbusException.getMessage().contains("3"));
        Assertions.assertTrue(modbusException.getMessage().contains("1"));
    }

    @Order(2)
    @Test
    @DisplayName("getExceptionCode")
    void checkGetExceptionCode(){
        Assertions.assertEquals(1, modbusException.getExceptionCode());
    }

    @Order(3)
    @Test
    @DisplayName("상수값")
    void checkConstant(){
        Assertions.assertAll(
                ()->Assertions.assertEquals(0x01, ModbusException.ILLEGAL_FUNCTION),
                ()->Assertions.assertEquals(0x02, ModbusException.ILLEGAL_DATA_ADDRESS),
                ()->Assertions.assertEquals(0x03, ModbusException.ILLEGAL_DATA_VALUE),
                ()->Assertions.assertEquals(0x04, ModbusException.SLAVE_DEVICE_FAILURE)
        );
    }
}
