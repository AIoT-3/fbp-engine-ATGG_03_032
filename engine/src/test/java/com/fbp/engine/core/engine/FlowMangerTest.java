package com.fbp.engine.core.engine;

import com.fbp.engine.core.parser.FlowDefinition;
import com.fbp.engine.core.parser.FlowParser;
import com.fbp.engine.core.parser.JsonFlowParser;
import com.fbp.engine.core.registry.NodeFactory;
import com.fbp.engine.core.registry.NodeRegistry;
import com.fbp.engine.node.internal.CollectorNode;
import com.fbp.engine.node.internal.PrintNode;
import com.fbp.engine.node.internal.TemperatureSensorNode;
import com.fbp.engine.node.internal.TimerNode;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class FlowMangerTest {
    FlowManager flowManager;


    String jsonString;
    InputStream inputStream;
    FlowParser flowParser;
    FlowDefinition flowDefinition;
    NodeRegistry nodeRegistry;

    @BeforeEach
    void setUp(){
        jsonString = """
                {
                    "id": "temperature-sensor-collect",
                    "name": "온도 센서 수집 플로우",
                    "description": "온도 센서 데이터를 생성하여 수집",
                    "nodes": [
                    {
                       "id": "trigger",
                       "type": "Timer",
                       "config": {
                        "intervalMs": 50
                       }
                    },
                    {
                        "id": "temperature-gener",
                        "type": "TemperatureSensor",
                        "config": {
                            "min": 25.0,
                            "max": 40.0
                        }
                    },
                    {
                        "id": "collector",
                        "type": "Collector"
                    },
                    {
                        "id": "printer",
                        "type": "Printer"
                    }
                    ],
                    "connections": [
                        {"from": "trigger:out", "to": "temperature-gener:trigger"},
                        {"from": "temperature-gener:out", "to": "collector:in"},
                        {"from": "temperature-gener:out", "to": "printer:in"}
                    ]
                }
                """;
        inputStream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
        flowParser = new JsonFlowParser();
        flowDefinition = flowParser.parse(inputStream);

        nodeRegistry = new NodeRegistry();

        NodeFactory timerFactory = (id, config) -> {
            return new TimerNode(id, ((Number)config.get("intervalMs")).longValue());
        };
        nodeRegistry.register("Timer", timerFactory);

        NodeFactory temperatureSensorFactory = (id, config) -> {
            return new TemperatureSensorNode(id, ((Number)config.get("min")).doubleValue(), ((Number)config.get("max")).doubleValue());
        };
        nodeRegistry.register("TemperatureSensor", temperatureSensorFactory);

        NodeFactory collectorFactory = (id, config) -> {
            return new CollectorNode(id);
        };
        nodeRegistry.register("Collector", collectorFactory);

        NodeFactory printerFactory = (id, config) -> {
            return new PrintNode(id);
        };
        nodeRegistry.register("Printer", printerFactory);

        flowManager = new FlowManager(nodeRegistry);
    }

    @Order(1)
    @Test
    @DisplayName("deploy")
    void checkDeploy(){
        flowManager.deploy(flowDefinition);

        Assertions.assertTrue(flowManager.getStatus(flowDefinition.getId()).equals(State.RUNNING));

        flowManager.remove(flowDefinition.getId());
    }

    @Order(2)
    @Test
    @DisplayName("list")
    void checkDeployedFlowList(){
        flowManager.deploy(flowDefinition);

        for(String flowId: flowManager.getDeployedFlowList()){
            Assertions.assertEquals(flowDefinition.getId(), flowId);
        }

        flowManager.remove(flowDefinition.getId());
    }

    @Order(3)
    @Test
    @DisplayName("getStatus")
    void checkGetStatus(){
        flowManager.deploy(flowDefinition);
        Assertions.assertTrue(flowManager.getStatus(flowDefinition.getId()).equals(State.RUNNING));

        flowManager.stop(flowDefinition.getId());
        Assertions.assertTrue(flowManager.getStatus(flowDefinition.getId()).equals(State.STOPPED));

        flowManager.restart(flowDefinition.getId());
        Assertions.assertTrue(flowManager.getStatus(flowDefinition.getId()).equals(State.RUNNING));

        flowManager.remove(flowDefinition.getId());
    }

    @Order(4)
    @Test
    @DisplayName("stop")
    void checkStop(){
        flowManager.deploy(flowDefinition);
        Assertions.assertTrue(flowManager.getStatus(flowDefinition.getId()).equals(State.RUNNING));

        flowManager.stop(flowDefinition.getId());
        Assertions.assertTrue(flowManager.getStatus(flowDefinition.getId()).equals(State.STOPPED));

        flowManager.remove(flowDefinition.getId());
    }

    @Order(5)
    @Test
    @DisplayName("restart")
    void checkRestart(){
        flowManager.deploy(flowDefinition);
        Assertions.assertTrue(flowManager.getStatus(flowDefinition.getId()).equals(State.RUNNING));

        flowManager.stop(flowDefinition.getId());
        Assertions.assertTrue(flowManager.getStatus(flowDefinition.getId()).equals(State.STOPPED));

        flowManager.restart(flowDefinition.getId());
        Assertions.assertTrue(flowManager.getStatus(flowDefinition.getId()).equals(State.RUNNING));

        flowManager.remove(flowDefinition.getId());
    }

    @Order(6)
    @Test
    @DisplayName("remove")
    void checkRemove(){
        flowManager.deploy(flowDefinition);
        Assertions.assertTrue(flowManager.getStatus(flowDefinition.getId()).equals(State.RUNNING));

        flowManager.stop(flowDefinition.getId());
        Assertions.assertTrue(flowManager.getStatus(flowDefinition.getId()).equals(State.STOPPED));

        flowManager.remove(flowDefinition.getId());
        Assertions.assertEquals(0, flowManager.getDeployedFlowList().size());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            flowManager.getStatus(flowDefinition.getId());
        });
    }

    @Order(7)
    @Test
    @DisplayName("실행중 삭제")
    void checkRemoveWhenRunningState(){
        flowManager.deploy(flowDefinition);
        Assertions.assertTrue(flowManager.getStatus(flowDefinition.getId()).equals(State.RUNNING));

        flowManager.remove(flowDefinition.getId());
        Assertions.assertEquals(0, flowManager.getDeployedFlowList().size());

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            flowManager.getStatus(flowDefinition.getId());
        });
    }

    @Order(8)
    @Test
    @DisplayName("존재 하지 않는 id 조작")
    void checkNotExistsIdControl(){
        Assertions.assertThrows(Exception.class, ()->{
            flowManager.stop("notExists");
        });

        Assertions.assertThrows(Exception.class, ()->{
            flowManager.restart("notExists");
        });

        Assertions.assertThrows(Exception.class, ()->{
            flowManager.remove("notExists");
        });
    }

    @Order(9)
    @Test
    @DisplayName("중복 id 배포")
    void checkDuplicatedIdDeploy(){
        flowManager.deploy(flowDefinition);

        Assertions.assertThrows(Exception.class, ()->{
            flowManager.deploy(flowDefinition);
        });

        flowManager.remove(flowDefinition.getId());
    }

    @Order(10)
    @Test
    @DisplayName("미등록 노드 타입")
    void checkIfFlowDefinitionContainsNodeRegistryUndefinedNodeTypeThenThrowException(){
        jsonString = """
                {
                    "id": "temperature-sensor-collect",
                    "name": "온도 센서 수집 플로우",
                    "description": "온도 센서 데이터를 생성하여 수집",
                    "nodes": [
                    {
                       "id": "trigger",
                       "type": "Timer",
                       "config": {
                        "intervalMs": 50
                       }
                    },
                    {
                        "id": "temperature-gener",
                        "type": "TemperatureSensor",
                        "config": {
                            "min": 25.0,
                            "max": 40.0
                        }
                    },
                    {
                        "id": "collector",
                        "type": "Collector"
                    },
                    {
                        "id": "printer",
                        "type": "Printer"
                    }
                    ],
                    "connections": [
                        {"from": "trigger:out", "to": "temperature-gener:trigger"},
                        {"from": "temperature-gener:out", "to": "collector:in"},
                        {"from": "temperature-gener:out", "to": "printer:in"}
                    ]
                }
                """;
        inputStream = new ByteArrayInputStream(jsonString.getBytes(StandardCharsets.UTF_8));
        flowParser = new JsonFlowParser();
        flowDefinition = flowParser.parse(inputStream);

        nodeRegistry = new NodeRegistry();

        flowManager = new FlowManager(nodeRegistry);

        Assertions.assertThrows(Exception.class, ()->{
           flowManager.deploy(flowDefinition);
        });
    }
}
