package com.fbp.engine.node.impl;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class HumiditySensorNodeTest {
    HumiditySensorNode target;
    Connection connModule;

    @BeforeEach
    void setUp(){
        target = new HumiditySensorNode("target", 30, 90);
        connModule = new Connection("conn-module");
        target.getOutputPort("out").connect(connModule);
    }

    @Order(1)
    @Test
    @DisplayName("습도 범위 확인")
    void checkHumidityRange(){
        for(int i=0; i<5; i++){
            target.process("trigger", new Message(Map.of("test","value")));
        }

        List<Double> createdValue = new ArrayList<>();

        for(int i=0; i<5; i++){
            Message poll = connModule.poll();
            if(poll != null) {
                createdValue.add(poll.get("humidity"));
            }
        }

        for(Double humidity: createdValue){
            Assertions.assertTrue(humidity>=30.0 && humidity<=90.0);
        }
    }

    @Order(2)
    @Test
    @DisplayName("필수 키 포함")
    void checkRequiredKey(){
        for(int i=0; i<5; i++){
            target.process("trigger", new Message(Map.of("test","value")));
        }

        List<Message> createdMessage = new ArrayList<>();

        for(int i=0; i<5; i++){
            createdMessage.add(connModule.poll());
        }

        for(Message message: createdMessage){
            Assertions.assertTrue(
                    message.hasKey("sensorId") &&
                            message.hasKey("humidity") &&
                            message.hasKey("unit")
            );
        }
    }

    @Order(3)
    @Test
    @DisplayName("sensorId 일치")
    void checkCreatedMsgSensorId(){
        for(int i=0; i<5; i++){
            target.process("trigger", new Message(Map.of("test","value")));
        }

        List<Message> createdMessage = new ArrayList<>();

        for(int i=0; i<5; i++){
            createdMessage.add(connModule.poll());
        }

        for(Message message: createdMessage){
            Assertions.assertEquals("target", message.get("sensorId"));
        }
    }

    @Order(4)
    @Test
    @DisplayName("트리거마다 생성")
    void checkCreatedPerTrigger(){
        for(int i=0; i<3; i++){
            target.process("trigger", new Message(Map.of("test","value")));
        }

        List<Message> createdMessage = new ArrayList<>();

        for(int i=0; i<3; i++){
            Message poll = connModule.poll();
            if(poll!=null) {
                createdMessage.add(poll);
            }
        }

        Assertions.assertEquals(3, createdMessage.size());
    }


}
