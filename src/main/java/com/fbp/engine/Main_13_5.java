package com.fbp.engine;

import com.fbp.engine.protocol.modbus.ModbusTcpClient;
import com.fbp.engine.protocol.modbus.ModbusTcpSimulator;
import com.fbp.engine.protocol.modbus.exception.ModbusException;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;

public class Main_13_5 {
    public static void main(String[] args) throws InterruptedException, IOException, ModbusException {
        ModbusTcpSimulator simulator = new ModbusTcpSimulator(5020, 10);
        simulator.setRegister(0, 250);
        simulator.setRegister(1, 600);
        simulator.setRegister(2, 1);

        simulator.start();

        ModbusTcpClient client = new ModbusTcpClient(InetAddress.getLoopbackAddress().getHostAddress(),5020);
        client.connect();

        int[] beforeRegisters = client.readHoldingRegisters(1, 0, 3);
        System.out.println(Arrays.toString(beforeRegisters));

        client.writeSingleRegister(1, 2, 100);

        int[] afterRegisters = client.readHoldingRegisters(1, 0, 3);
        System.out.println(Arrays.toString(afterRegisters));

        Thread.sleep(200);

        client.disconnect();
        simulator.stop();
    }
}
