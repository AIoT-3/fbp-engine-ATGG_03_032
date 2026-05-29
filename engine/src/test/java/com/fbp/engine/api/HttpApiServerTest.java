package com.fbp.engine.api;

import com.fbp.engine.core.engine.FlowManager;
import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.core.parser.FlowDefinition;
import com.fbp.engine.core.parser.NodeDefinition;
import com.fbp.engine.core.registry.NodeRegistry;
import com.fbp.engine.metrics.MetricsCollector;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpApiServerTest {

    private HttpApiServer server;
    private int port;
    private HttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final FlowManager flowManager = FlowManager.getInstance();
    private final MetricsCollector metricsCollector = MetricsCollector.getInstance();

    @BeforeAll
    void beforeAll() throws IOException {
        try (java.net.ServerSocket s = new java.net.ServerSocket(0)) {
            port = s.getLocalPort();
        }

        NodeRegistry registry = flowManager.getNodeRegistry();
        registry.register("com.fbp.engine.nodes.internal.StartNode", (id, config) -> new AbstractNode(id) {
            @Override
            public void onProcess(String portName, com.fbp.engine.message.Message message) {
            }
        });

        server = new HttpApiServer("localhost", port);
        server.start();
        client = HttpClient.newHttpClient();
    }

    @AfterAll
    void afterAll() {
        if (server != null) {
            server.stop(0);
        }
        flowManager.reset();
        metricsCollector.resetAll();
    }

    @BeforeEach
    void setUp() {
        flowManager.getDeployedFlowList().forEach(flowManager::remove);
        metricsCollector.resetAll();
    }

    @Test
    @DisplayName("서버 시작/정지: start() → 포트 바인딩 확인, stop() → 정상 종료")
    void testServerStartStop() throws IOException, InterruptedException {
        HttpResponse<String> response = sendGetRequest("/health");
        assertEquals(200, response.statusCode());

        server.stop(0);
        assertThrows(ConnectException.class, () -> sendGetRequest("/health"));

        server = new HttpApiServer("localhost", port);
        server.start();
    }

    @Test
    @DisplayName("GET /health: 200 OK, status 필드 포함")
    void testGetHealth() throws IOException, InterruptedException {
        HttpResponse<String> response = sendGetRequest("/health");
        assertEquals(200, response.statusCode());
        Map<String, Object> health = objectMapper.readValue(response.body(), new TypeReference<>() {});
        assertEquals("UP", health.get("status"));
        assertTrue((Integer) health.get("flowCount") >= 0);
    }

    @Test
    @DisplayName("GET /flows: 200 OK, 배포된 플로우 목록 반환")
    void testGetFlows() throws IOException, InterruptedException {
        flowManager.deploy(createDummyFlow("flow-1"));

        HttpResponse<String> response = sendGetRequest("/flows");
        assertEquals(200, response.statusCode());
        List<Map<String, Object>> flows = objectMapper.readValue(response.body(), new TypeReference<>() {});
        assertEquals(1, flows.size());
        assertEquals("flow-1", flows.get(0).get("id"));
    }

    @Test
    @DisplayName("POST /flows: 유효한 JSON → 201 Created, 플로우 배포 확인")
    void testPostFlow() throws IOException, InterruptedException {
        FlowDefinition flowDef = createDummyFlow("flow-2");
        String jsonBody = objectMapper.writeValueAsString(flowDef);

        HttpResponse<String> response = sendPostRequest("/flows", jsonBody);
        assertEquals(201, response.statusCode());
        
        Map<String, Object> result = objectMapper.readValue(response.body(), new TypeReference<>() {});
        assertEquals("flow-2", result.get("id"));

        assertEquals(1, flowManager.getDeployedFlowList().size());
    }

    @Test
    @DisplayName("POST /flows 잘못된 JSON: 400 Bad Request")
    void testPostFlow_InvalidJson() throws IOException, InterruptedException {
        HttpResponse<String> response = sendPostRequest("/flows", "{'invalid': 'json'}");
        assertEquals(400, response.statusCode());
    }

    @Test
    @DisplayName("DELETE /flows/{id}: 존재하는 플로우 삭제 → 200 OK")
    void testDeleteFlow() throws IOException, InterruptedException {
        flowManager.deploy(createDummyFlow("flow-to-delete"));
        
        HttpResponse<String> response = sendDeleteRequest("/flows/flow-to-delete");
        assertEquals(200, response.statusCode());

        assertTrue(flowManager.getDeployedFlowList().isEmpty());
    }

    @Test
    @DisplayName("DELETE /flows/{id} 없는 id: 404 Not Found")
    void testDeleteFlow_NotFound() throws IOException, InterruptedException {
        HttpResponse<String> response = sendDeleteRequest("/flows/non-existent");
        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("GET /flows/{id}/metrics: 배포된 플로우의 메트릭 JSON 반환")
    void testGetFlowMetrics() throws IOException, InterruptedException {
        metricsCollector.recordProcessing("flow-metrics", "node-a", 100, true);
        
        HttpResponse<String> response = sendGetRequest("/flows/flow-metrics/metrics");
        assertEquals(200, response.statusCode());
        
        Map<String, Object> metrics = objectMapper.readValue(response.body(), new TypeReference<>() {});
        assertTrue(metrics.containsKey("nodes"));
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) metrics.get("nodes");
        assertEquals(1, nodes.size());
        assertEquals("node-a", nodes.get(0).get("nodeId"));
    }

    @Test
    @DisplayName("존재하지 않는 경로: 404 Not Found")
    void testNotFoundPath() throws IOException, InterruptedException {
        HttpResponse<String> response = sendGetRequest("/non/existent/path");
        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("잘못된 HTTP 메서드: 405 Method Not Allowed")
    void testMethodNotAllowed() throws IOException, InterruptedException {
        HttpResponse<String> response = sendPostRequest("/health", "");
        assertEquals(405, response.statusCode());
    }
    
    @Test
    @DisplayName("Content-Type: 응답 헤더에 application/json 포함")
    void testContentTypeHeader() throws IOException, InterruptedException {
        HttpResponse<String> response = sendGetRequest("/health");
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("application/json"));
    }

    private HttpResponse<String> sendGetRequest(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendPostRequest(String path, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .header("Content-Type", "application/json")
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> sendDeleteRequest(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .DELETE()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
    
    private FlowDefinition createDummyFlow(String id) {
        NodeDefinition nodeDef = new NodeDefinition("node1", "com.fbp.engine.nodes.internal.StartNode", Collections.emptyMap());
        return new FlowDefinition(id, "dummy", "", List.of(nodeDef), Collections.emptyList());
    }
}
