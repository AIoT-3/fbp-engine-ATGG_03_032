package com.fbp.engine.node.external;

import com.fbp.engine.core.node.ProtocolNode;
import com.fbp.engine.message.Message;
import com.fbp.engine.protocol.modbus.ModbusTcpClient;
import com.fbp.engine.protocol.modbus.exception.ModbusException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/*
config keys::
host(String),
port(int, 기본 502),
slaveId(int),
startAddress(int),
count(int),
registerMapping(Map<String, Object>, 선택)
 */
@Slf4j
public class ModbusReaderNode extends ProtocolNode {
    private static final int DEFAULT_PORT = 502;
    private ModbusTcpClient client;

    public ModbusReaderNode(String id, Map<String, Object> config) {
        super(id, config);

        if (config.get("host") == null){
            throw new IllegalArgumentException("host must be notNull");
        }
        if (config.get("slaveId") == null){
            throw new IllegalArgumentException("slaveId must be notNull");
        }
        if (config.get("startAddress") == null){
            throw new IllegalArgumentException("startAddress must be notNull");
        }
        if (config.get("count") == null){
            throw new IllegalArgumentException("count must be notNull");
        }

        addInputPort("trigger");
        addOutputPort("out");
        addOutputPort("error");
    }


    @Override
    protected void connect() throws Exception {
        client = new ModbusTcpClient((String) getConfig("host"),
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
        int slaveId = (Integer) getConfig("slaveId");
        int startAddress = (Integer) getConfig("startAddress");
        int count = (Integer) getConfig("count");

        try {
            int[] result = client.readHoldingRegisters(slaveId, startAddress, count);

            Map<String,Object> registerMapping = (Map<String, Object>) getConfig("registerMapping");
            Map<String,Object> registers = new HashMap<>();

            Set<Integer> mappedIndexes = new HashSet<>();
            for(Map.Entry<String,Object> entry : registerMapping.entrySet()){
                String fieldName = entry.getKey();
                Map<String, Object> meta = (Map<String, Object>) entry.getValue();

                int index = (Integer) meta.get("index");
                if(index<0 || index >= result.length){
                    continue;
                }

                double value = result[index] & 0xFFFF;
                if(meta.containsKey("scale")){
                    value *= ((Number) meta.get("scale")).doubleValue();
                }
                registers.put(fieldName, value);
                mappedIndexes.add(index);
            }
            for(int i=0; i<result.length; i++){
                if(!mappedIndexes.contains(i)){
                    registers.put(
                            String.format("register[%d]", startAddress + i),
                            result[i] & 0xFFFF
                    );
                }
            }
            Message resultMessage = new Message(registers);

            send("out", resultMessage);
        }catch (ModbusException | IOException e){
            log.error("Modbus read failed", e);

            String errorMsg = (e.getMessage() != null) ? e.getMessage() : e.toString();

            send("error", new Message(Map.of("errorMsg", errorMsg)));
        }
    }
}
