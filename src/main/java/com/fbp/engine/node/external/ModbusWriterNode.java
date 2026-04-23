package com.fbp.engine.node.external;

import com.fbp.engine.core.node.ProtocolNode;
import com.fbp.engine.message.Message;
import com.fbp.engine.protocol.modbus.ModbusTcpClient;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/*
host(String),
port(int, 기본 502),
slaveId(int),
registerAddress(int),
valueField(String — FBP Message에서 값을 읽을 키),
scale(double, 기본 1.0)
 */
@Slf4j
public class ModbusWriterNode extends ProtocolNode {
    private static final int DEFAULT_PORT = 502;
    private static final double DEFAULT_SCALE = 1.0;

    private ModbusTcpClient client;

    public ModbusWriterNode(String id, Map<String, Object> config) {
        super(id, config);

        if (config.get("host") == null){
            throw new IllegalArgumentException("host must be notNull");
        }
        if (config.get("slaveId") == null){
            throw new IllegalArgumentException("slaveId must be notNull");
        }
        if (config.get("registerAddress") == null){
            throw new IllegalArgumentException("registerAddress must be notNull");
        }
        if (config.get("valueField") == null){
            throw new IllegalArgumentException("valueField must be notNull");
        }

        addInputPort("in");
        addOutputPort("result");
    }

    @Override
    protected void connect() throws Exception {
        this.client = new ModbusTcpClient((String) getConfig("host"),
                getConfig("port") != null ? (int) getConfig("port") : DEFAULT_PORT);
        client.connect();
    }

    @Override
    protected void disconnect() {
        if(client != null && client.isConnected()){
            client.disconnect();
        }
    }

    @Override
    public void onProcess(String portName, Message message) {
        Integer slaveId = (Integer) getConfig("slaveId");
        Integer registerAddress = (Integer) getConfig("registerAddress");
        Number value = message.get((String) getConfig("valueField"));
        Number scale = getConfig("scale") != null ? (Number) getConfig("scale") : DEFAULT_SCALE;
        Integer forWriteValue = Integer.valueOf((int) (value.doubleValue() * scale.doubleValue()));

        try {
            client.writeSingleRegister(slaveId,
                    registerAddress,
                    forWriteValue);

            Message resultMessage = new Message(Map.of(
                    "slaveId", slaveId,
                    "registerAddress", registerAddress,
                    "writtenValue", forWriteValue
            ));
            send("result", resultMessage);
        } catch (Exception e) {
            log.error("{}", e.getMessage(), e);
        }

    }
}
