package com.fbp.engine.core.flow;

import com.fbp.engine.message.Message;
import com.fbp.engine.node.impl.AbstractNode;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FlowEngineTest {
    FlowEngine flowEngine;

    @Mock
    Flow flow;

    @Mock
    AbstractNode abstractNode;

    private final String TEST_FLOW_ID = "test-flow";

    @BeforeEach
    void setUp(){
        lenient().when(flow.getId()).thenReturn(TEST_FLOW_ID);

        flowEngine = new FlowEngine();

        flowEngine.register(flow);
        flow.addNode(abstractNode);
    }

    @Order(1)
    @Test
    @DisplayName("초기 상태")
    void initialState(){
        Assertions.assertEquals(FlowEngine.State.INITIALIZED, flowEngine.getState());
    }

    @Order(2)
    @Test
    @DisplayName("플로우 등록")
    void registerFlow(){
        Flow flow = new Flow("module");

        flowEngine.register(flow);

        Assertions.assertTrue(flowEngine.getFlows().containsValue(flow));
    }

    @Order(3)
    @Test
    @DisplayName("startFlow 정상")
    void startFlowSuccessfully(){
        when(flow.validate()).thenReturn(Collections.EMPTY_LIST);

        flowEngine.startFlow(TEST_FLOW_ID);

        Assertions.assertEquals(FlowEngine.State.RUNNING, flowEngine.getState());
    }

    @Order(4)
    @Test
    @DisplayName("startFlow - 없는 ID")
    void startFlowNotExistsId(){
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            flowEngine.startFlow("nothing");
        });
    }

    @Order(5)
    @Test
    @DisplayName("startFlow - 유효성 실패")
    void startFlowValidationFailed(){
        when(flow.validate()).thenReturn(List.of("err"));

        Assertions.assertThrows(IllegalStateException.class, () -> {
            flowEngine.startFlow(TEST_FLOW_ID);
        });
    }

    @Order(6)
    @Test
    @DisplayName("stopFlow 정상")
    void stopFlowSuccessfully(){
        when(flow.validate()).thenReturn(Collections.EMPTY_LIST);
        flowEngine.startFlow(TEST_FLOW_ID);

        flowEngine.stopFlow(TEST_FLOW_ID);

        verify(flow, times(1)).shutdown();
    }

    @Order(7)
    @Test
    @DisplayName("shutdown 전체")
    void shutdownAll(){
        when(flow.validate()).thenReturn(Collections.EMPTY_LIST);
        flowEngine.startFlow(TEST_FLOW_ID);

        flowEngine.shutdown();

        verify(flow, times(1)).shutdown();
    }

    @Order(8)
    @Test
    @DisplayName("다중 플로우 독립 동작")
    void multipleFlowIndependentOperation(){
        FlowEngine flowEngine = new FlowEngine();

        Flow flow = new Flow("flow");
        flow.addNode(new AbstractNode("module") {
            @Override
            public void onProcess(Message message) {}
        });
        Flow flow1 = new Flow("flow1");
        flow1.addNode(new AbstractNode("module") {
            @Override
            public void onProcess(Message message) {}
        });

        flowEngine.register(flow);
        flowEngine.register(flow1);

        Assertions.assertDoesNotThrow(()->{
            flowEngine.startFlow("flow");
            flowEngine.startFlow("flow1");
            flowEngine.stopFlow("flow");
        });
    }

    @Order(9)
    @Test
    @DisplayName("listFlows 출력")
    void printListFlows(){
        FlowEngine flowEngine = new FlowEngine();

        Flow flow = new Flow("flow");
        flow.addNode(new AbstractNode("module") {
            @Override
            public void onProcess(Message message) {}
        });
        Flow flow1 = new Flow("flow1");
        flow1.addNode(new AbstractNode("module") {
            @Override
            public void onProcess(Message message) {}
        });

        flowEngine.register(flow);
        flowEngine.register(flow1);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        System.setOut(new PrintStream(output));

        flowEngine.listFlows();

        String result = output.toString();

        Assertions.assertAll(
                ()-> Assertions.assertTrue(result.contains("flow")),
                ()-> Assertions.assertTrue(result.contains("flow1")),
                ()-> Assertions.assertTrue(result.contains("INITIALIZED"))
        );
    }
}
