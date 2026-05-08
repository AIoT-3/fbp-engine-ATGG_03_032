package com.fbp.engine.core.parser;

import com.fbp.engine.core.parser.*;
import org.junit.jupiter.api.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class JsonFlowParserTest {
    FlowParser flowParser;
    InputStream normalJsonStream;

    @BeforeEach
    void setUp(){
        flowParser = new JsonFlowParser();
        String normalJson = "{\n" +
                "  \"id\": \"temperature-monitoring\",\n" +
                "  \"name\": \"온도 모니터링 플로우\",\n" +
                "  \"description\": \"MQTT 센서 데이터를 수신하여 임계값 초과 시 알림\",\n" +
                "  \"nodes\": [\n" +
                "    {\n" +
                "      \"id\": \"sensor\",\n" +
                "      \"type\": \"MqttSubscriber\",\n" +
                "      \"config\": {\n" +
                "        \"broker\": \"tcp://localhost:1883\",\n" +
                "        \"topic\": \"sensor/temp\",\n" +
                "        \"qos\": 1\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"rule\",\n" +
                "      \"type\": \"ThresholdFilter\",\n" +
                "      \"config\": {\n" +
                "        \"field\": \"value\",\n" +
                "        \"operator\": \">\",\n" +
                "        \"threshold\": 30\n" +
                "      }\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"alert\",\n" +
                "      \"type\": \"MqttPublisher\",\n" +
                "      \"config\": {\n" +
                "        \"broker\": \"tcp://localhost:1883\",\n" +
                "        \"topic\": \"alert/temp\"\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"connections\": [\n" +
                "    { \"from\": \"sensor:out\", \"to\": \"rule:in\" },\n" +
                "    { \"from\": \"rule:out\", \"to\": \"alert:in\" }\n" +
                "  ]\n" +
                "}";
        normalJsonStream = new ByteArrayInputStream(normalJson.getBytes(StandardCharsets.UTF_8));
    }

    @Order(1)
    @Test
    @DisplayName("정상 파싱")
    void parseSuccessfully(){
        Assertions.assertDoesNotThrow(()->flowParser.parse(normalJsonStream));
    }

    @Order(2)
    @Test
    @DisplayName("노드 목록")
    void checkNodeList(){
        FlowDefinition parse = flowParser.parse(normalJsonStream);

        List<NodeDefinition> nodes = parse.getNodes();

        Assertions.assertAll(
                ()->Assertions.assertEquals(3, nodes.size()),

                ()->Assertions.assertAll(
                        ()->Assertions.assertEquals("sensor", nodes.get(0).getId()),
                        ()->Assertions.assertEquals("MqttSubscriber", nodes.get(0).getType()),
                        ()->Assertions.assertEquals("tcp://localhost:1883", nodes.get(0).getConfig().get("broker")),
                        ()->Assertions.assertEquals("sensor/temp", nodes.get(0).getConfig().get("topic")),
                        ()->Assertions.assertEquals(1, nodes.get(0).getConfig().get("qos"))
                ),

                ()->Assertions.assertAll(
                        ()->Assertions.assertEquals("rule", nodes.get(1).getId()),
                        ()->Assertions.assertEquals("ThresholdFilter", nodes.get(1).getType()),
                        ()->Assertions.assertEquals("value", nodes.get(1).getConfig().get("field")),
                        ()->Assertions.assertEquals(">", nodes.get(1).getConfig().get("operator")),
                        ()->Assertions.assertEquals(30, nodes.get(1).getConfig().get("threshold"))
                ),

                ()->Assertions.assertAll(
                        ()->Assertions.assertEquals("alert", nodes.get(2).getId()),
                        ()->Assertions.assertEquals("MqttPublisher", nodes.get(2).getType()),
                        ()->Assertions.assertEquals("tcp://localhost:1883", nodes.get(2).getConfig().get("broker")),
                        ()->Assertions.assertEquals("alert/temp", nodes.get(2).getConfig().get("topic"))
                )
        );
    }

    @Order(3)
    @Test
    @DisplayName("연결 목록")
    void checkConnectionList(){
        FlowDefinition parse = flowParser.parse(normalJsonStream);

        List<ConnectionDefinition> connections = parse.getConnections();

        Assertions.assertAll(
                ()->Assertions.assertTrue(connections.get(0).getFrom().equals("sensor:out") &&
                        connections.get(0).getTo().equals("rule:in")),
                ()->Assertions.assertTrue(connections.get(1).getFrom().equals("rule:out") &&
                        connections.get(1).getTo().equals("alert:in"))
        );
    }

    @Order(4)
    @Test
    @DisplayName("필수 필드 누락 - id")
    void checkIfFieldMissing(){
        String invalidJson = """
                {
                  "name": "온도 모니터링 플로우",
                  "description": "MQTT 센서 데이터를 수신하여 임계값 초과 시 알림",
                  "nodes": [
                    {
                      "id": "sensor",
                      "type": "MqttSubscriber",
                      "config": {
                        "broker": "tcp://localhost:1883",
                        "topic": "sensor/temp",
                        "qos": 1
                      }
                    },
                    {
                      "id": "rule",
                      "type": "ThresholdFilter",
                      "config": {
                        "field": "value",
                        "operator": ">",
                        "threshold": 30
                      }
                    },
                    {
                      "id": "alert",
                      "type": "MqttPublisher",
                      "config": {
                        "broker": "tcp://localhost:1883",
                        "topic": "alert/temp"
                      }
                    }
                  ],
                  "connections": [
                    { "from": "sensor:out", "to": "rule:in" },
                    { "from": "rule:out", "to": "alert:in" }
                  ]
                }
                """;
        InputStream invalidInputStream = new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

        Assertions.assertThrows(FlowParserException.class, ()->{
            flowParser.parse(invalidInputStream);
        });
    }

    @Order(5)
    @Test
    @DisplayName("필수 필드 누락 - nodes")
    void checkIfNodesMissing(){
        String invalidJson = """
                {
                  "id": "temperature-monitoring",
                  "name": "온도 모니터링 플로우",
                  "description": "MQTT 센서 데이터를 수신하여 임계값 초과 시 알림",
                  "connections": [
                    { "from": "sensor:out", "to": "rule:in" },
                    { "from": "rule:out", "to": "alert:in" }
                  ]
                }
                """;
        InputStream invalidInputStream = new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

        Assertions.assertThrows(FlowParserException.class, ()->{
            flowParser.parse(invalidInputStream);
        });
    }

    @Order(6)
    @Test
    @DisplayName("빈 노드 목록")
    void checkIfNodesEmpty(){
        String invalidJson = """
                "id": "temperature-monitoring",
                  "name": "온도 모니터링 플로우",
                  "description": "MQTT 센서 데이터를 수신하여 임계값 초과 시 알림",
                  "nodes": [],
                  "connections": [
                    { "from": "sensor:out", "to": "rule:in" },
                    { "from": "rule:out", "to": "alert:in" }
                  ]
                }
                """;
        InputStream invalidInputStream = new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

        Assertions.assertThrows(FlowParserException.class, ()->{
            flowParser.parse(invalidInputStream);
        });
    }

    @Order(7)
    @Test
    @DisplayName("잘못된 JSON 형식")
    void checkInvalidJsonForm(){
        String invalidJson = """
                1q2w3e4r!
                """;
        InputStream invalidInputStream = new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

        Assertions.assertThrows(FlowParserException.class, ()->{
            flowParser.parse(invalidInputStream);
        });
    }

    @Order(8)
    @Test
    @DisplayName("연결의 포트 파싱")
    void checkConnectionPortParsing(){
        FlowDefinition parse = flowParser.parse(normalJsonStream);

        List<ConnectionDefinition> connections = parse.getConnections();

        ConnectionDefinition connectionDefinition = connections.get(0);
        ConnectionDefinition connectionDefinition1 = connections.get(1);

        Assertions.assertAll(
                ()->Assertions.assertAll(
                        ()->Assertions.assertEquals("sensor:out", connectionDefinition.getFrom()),
                        ()->Assertions.assertEquals("rule:in", connectionDefinition.getTo())
                ),
                ()->Assertions.assertAll(
                        ()->Assertions.assertEquals("rule:out", connectionDefinition1.getFrom()),
                        ()->Assertions.assertEquals("alert:in", connectionDefinition1.getTo())
                )
        );
    }

    @Order(9)
    @Test
    @DisplayName("잘못된 연결 형식")
    void checkInvalidConnectionFormat(){
        String invalidJson = """
                {
                  "id": "temperature-monitoring",
                  "name": "온도 모니터링 플로우",
                  "description": "MQTT 센서 데이터를 수신하여 임계값 초과 시 알림",
                  "nodes": [
                    {
                      "id": "sensor",
                      "type": "MqttSubscriber",
                      "config": {
                        "broker": "tcp://localhost:1883",
                        "topic": "sensor/temp",
                        "qos": 1
                      }
                    },
                    {
                      "id": "rule",
                      "type": "ThresholdFilter",
                      "config": {
                        "field": "value",
                        "operator": ">",
                        "threshold": 30
                      }
                    },
                    {
                      "id": "alert",
                      "type": "MqttPublisher",
                      "config": {
                        "broker": "tcp://localhost:1883",
                        "topic": "alert/temp"
                      }
                    }
                  ],
                  "connections": [
                    { "from": "sensor:", "to": "rule:in" },
                    { "from": "rule:out", "to": "alert:" }
                  ]
                }
                """;
        InputStream inputStream = new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

        Assertions.assertThrows(FlowParserException.class, ()->flowParser.parse(inputStream));
    }

    @Order(10)
    @Test
    @DisplayName("존재하지 않는 노드 참조")
    void checkConnectionReferencedNotDefinedNodeId(){
        String invalidJson = """
                {
                  "id": "temperature-monitoring",
                  "name": "온도 모니터링 플로우",
                  "description": "MQTT 센서 데이터를 수신하여 임계값 초과 시 알림",
                  "nodes": [
                    {
                      "id": "sensor",
                      "type": "MqttSubscriber",
                      "config": {
                        "broker": "tcp://localhost:1883",
                        "topic": "sensor/temp",
                        "qos": 1
                      }
                    },
                    {
                      "id": "rule",
                      "type": "ThresholdFilter",
                      "config": {
                        "field": "value",
                        "operator": ">",
                        "threshold": 30
                      }
                    },
                    {
                      "id": "alert",
                      "type": "MqttPublisher",
                      "config": {
                        "broker": "tcp://localhost:1883",
                        "topic": "alert/temp"
                      }
                    }
                  ],
                  "connections": [
                    { "from": "unknown:out", "to": "rule:in" },
                    { "from": "rule:out", "to": "alert:in" }
                  ]
                }
                """;

        InputStream inputStream = new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

        Assertions.assertThrows(FlowParserException.class, ()-> flowParser.parse(inputStream));
    }

    @Order(11)
    @Test
    @DisplayName("중복 노드 id")
    void checkDuplicatedNodeId(){
        String invalidJson = """
                {
                  "id": "temperature-monitoring",
                  "name": "온도 모니터링 플로우",
                  "description": "MQTT 센서 데이터를 수신하여 임계값 초과 시 알림",
                  "nodes": [
                    {
                      "id": "duplicate",
                      "type": "MqttSubscriber",
                      "config": {
                        "broker": "tcp://localhost:1883",
                        "topic": "sensor/temp",
                        "qos": 1
                      }
                    },
                    {
                      "id": "duplicate",
                      "type": "ThresholdFilter",
                      "config": {
                        "field": "value",
                        "operator": ">",
                        "threshold": 30
                      }
                    },
                    {
                      "id": "duplicate",
                      "type": "MqttPublisher",
                      "config": {
                        "broker": "tcp://localhost:1883",
                        "topic": "alert/temp"
                      }
                    }
                  ],
                  "connections": [
                    { "from": "sensor:out", "to": "rule:in" },
                    { "from": "rule:out", "to": "alert:in" }
                  ]
                }
                """;
        InputStream inputStream = new ByteArrayInputStream(invalidJson.getBytes(StandardCharsets.UTF_8));

        Assertions.assertThrows(FlowParserException.class, ()->flowParser.parse(inputStream));
    }

    @Order(12)
    @Test
    @DisplayName("config 타입 보존")
    void checkConfigTypePreservation(){
        FlowDefinition parse = flowParser.parse(normalJsonStream);
        List<NodeDefinition> nodes = parse.getNodes();

        Map<String, Object> config = nodes.get(0).getConfig();
        Map<String, Object> config1 = nodes.get(1).getConfig();
        Map<String, Object> config2 = nodes.get(2).getConfig();

        Assertions.assertAll(
                ()->Assertions.assertAll(
                        ()->Assertions.assertTrue(config.get("broker") instanceof String),
                        ()->Assertions.assertTrue(config.get("topic") instanceof String),
                        ()->Assertions.assertTrue(config.get("qos") instanceof Number)
                ),

                ()->Assertions.assertAll(
                        ()->Assertions.assertTrue(config1.get("field") instanceof String),
                        ()->Assertions.assertTrue(config1.get("operator") instanceof String),
                        ()->Assertions.assertTrue(config1.get("threshold") instanceof Number)
                ),

                ()->Assertions.assertAll(
                        ()->Assertions.assertTrue(config2.get("broker") instanceof String),
                        ()->Assertions.assertTrue(config2.get("topic") instanceof String)
                )
        );
    }
}
