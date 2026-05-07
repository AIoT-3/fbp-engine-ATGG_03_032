package com.fbp.engine.registry;

import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.external.ModbusReaderNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class NodeFactoryTest {
    @Order(1)
    @Test
    @DisplayName("정상 생성")
    void generationSuccessTest(){
        NodeFactory nodeFactory = (id, config) -> {
            return new AbstractNode(id) {
                @Override
                public void onProcess(String portName, Message message) {return;}
            };
        };

        Assertions.assertTrue(nodeFactory.create("id", null) instanceof AbstractNode);
    }

    @Order(2)
    @Test
    @DisplayName("잘못된 config")
    void wrongConfigTest(){
        NodeFactory modbusReaderNodeFactory = (id, config) ->{
            return new ModbusReaderNode(id, config);
        };

        Assertions.assertThrows(Exception.class, ()->{
            modbusReaderNodeFactory.create("id", Map.of());
        });
    }

    @Order(3)
    @Test
    @DisplayName("함수형 인터페이스로 람다 기반 팩토리 등록 가능")
    void lambdaBasedFactoryTest(){
        Assertions.assertDoesNotThrow(()->{
            NodeFactory nodeFactory = (id, config) -> {
                return new AbstractNode(id) {
                    @Override
                    public void onProcess(String portName, Message message) {return;}
                };
            };
        });
    }
}
