package com.fbp.engine.protocol.modbus.exception;

public class ModbusException extends Exception {
    public static final int ILLEGAL_FUNCTION = 0x01;
    public static final int ILLEGAL_DATA_ADDRESS = 0x02;
    public static final int ILLEGAL_DATA_VALUE = 0x03;
    public static final int SLAVE_DEVICE_FAILURE = 0x04;

    private int functionCode;
    private int exceptionCode;

    public ModbusException(int functionCode, int exceptionCode){
        this.functionCode = functionCode;
        this.exceptionCode = exceptionCode;
    }

    @Override
    public String getMessage() {
        return String.format("MODBUS ERROR - FC: 0x%02X, Exception: 0x$02X (%s)",
                functionCode, exceptionCode, getExceptionDescription());
    }

    public int getExceptionCode() {
        return exceptionCode;
    }

    public ModbusException(String message) {
        super(message);
    }

    private String getExceptionDescription(){
        if(exceptionCode==ILLEGAL_FUNCTION){
            return "ILLEGAL_FUNCTION";
        }
        if(exceptionCode==ILLEGAL_DATA_ADDRESS){
            return "ILLEGAL_DATA_ADDRESS";
        }
        if(exceptionCode==ILLEGAL_DATA_VALUE){
            return "ILLEGAL_DATA_VALUE";
        }
        if(exceptionCode==SLAVE_DEVICE_FAILURE){
            return "SLAVE_DEVICE_FAILURE";
        }

        return null;
    }
}
