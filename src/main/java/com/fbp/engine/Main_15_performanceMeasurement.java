package com.fbp.engine;

import ch.qos.logback.core.testUtil.RandomUtil;
import com.fbp.engine.core.flow.Flow;
import com.fbp.engine.core.flow.FlowEngine;
import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.core.rule.RuleExpression;
import com.fbp.engine.message.Message;
import com.fbp.engine.node.external.ModbusWriterNode;
import com.fbp.engine.node.external.MqttPublisherNode;
import com.fbp.engine.node.external.MqttSubscriberNode;
import com.fbp.engine.node.internal.RuleNode;
import com.fbp.engine.protocol.modbus.ModbusTcpSimulator;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;

public class Main_15_performanceMeasurement {
    private static final List<Long> latencies = new CopyOnWriteArrayList<>();
    private static final AtomicLong processedCount = new AtomicLong();

    public static void main(String[] args) throws InterruptedException {
        int modbusPort = RandomUtil.getRandomServerPort();
        ModbusTcpSimulator modbusTcpSimulator = new ModbusTcpSimulator(modbusPort, 30);

        MqttPublisherNode scenarioStub = new MqttPublisherNode("stub",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-pub",
                        "topic", "sensor/temp",
                        "qos", 1,
                        "retained", false));

        MqttSubscriberNode mqttSubscriberNode = new MqttSubscriberNode("subscriber",
                Map.of("brokerUrl", "tcp://localhost:1883",
                        "clientId", "test-sub",
                        "topic", "sensor/temp",
                        "qos", 1));

        RuleNode ruleNode = new RuleNode("ruler", message -> {
            RuleExpression parse = RuleExpression.parse("value > 30");
            return parse.evaluate(message);
        });

        ModbusWriterNode modbusWriterNode = new ModbusWriterNode("modbus-writer",
                Map.of("host", InetAddress.getLoopbackAddress().getHostAddress(),
                        "port", modbusPort,
                        "slaveId", 1,
                        "registerAddress", 2,
                        "valueField", "value"));

        AbstractNode metricsNode = new AbstractNode("metrics") {
            {addInputPort("in");}
            @Override
            public void onProcess(String portName, Message message) {
                Long sendTime = (Long) message.get("sendTime");
                if (sendTime != null) {
                    latencies.add(System.nanoTime() - sendTime);
                }
                processedCount.incrementAndGet();
            }
        };


        modbusTcpSimulator.start();
        Flow flow = new Flow("flow");

        flow.addNode(scenarioStub)
                .addNode(mqttSubscriberNode)
                .addNode(ruleNode)
                .addNode(modbusWriterNode)
                .addNode(metricsNode)
                .connect(mqttSubscriberNode.getId(), "out", ruleNode.getId(), "in")
                .connect(ruleNode.getId(), "match", modbusWriterNode.getId(), "in")
                .connect(mqttSubscriberNode.getId(), "out", metricsNode.getId(), "in");

        FlowEngine flowEngine = new FlowEngine();
        flowEngine.register(flow);
        flowEngine.startFlow("flow");

        List<String> benchmarkResult = new ArrayList<>();

        System.out.println("=== 워밍업 중... ===");
        runBenchmark(scenarioStub, 100, 2000);
        int[] rates = {100, 500, 1000};
        for (int rate : rates) {
            latencies.clear();
            processedCount.set(0);

            System.out.printf("%n=== %d msg/s 테스트 시작 ===%n", rate);
            long start = System.currentTimeMillis();
            runBenchmark(scenarioStub, rate, 10_000);
            long duration = System.currentTimeMillis() - start;

            Thread.sleep(500);
            benchmarkResult.add(printStats(rate, duration));
        }

        flowEngine.shutdown();
        modbusTcpSimulator.stop();

        Thread.sleep(100);

        for(String result: benchmarkResult){
            System.out.println(result);
        }
    }

    private static void runBenchmark(MqttPublisherNode stub, int targetMsgPerSec, int durationMs)
            throws InterruptedException {
        long intervalNs = 1_000_000_000L / targetMsgPerSec;
        long endTime = System.nanoTime() + durationMs * 1_000_000L;

        while (System.nanoTime() < endTime) {
            long sendTime = System.nanoTime();
            double value = RandomGenerator.getDefault().nextInt(20) + 20.0;

            stub.process("tmp", new Message(Map.of(
                    "value", value,
                    "sendTime", sendTime
            )));

            long next = sendTime + intervalNs;
            while (System.nanoTime() < next) {
                Thread.onSpinWait();
            }
        }
    }

    private static String printStats(int rate, long durationMs) {
        long total = processedCount.get();
        double throughput = total / (durationMs / 1000.0);

        List<Long> sorted = latencies.stream().sorted().toList();
        LongSummaryStatistics stats = sorted.stream().mapToLong(Long::longValue).summaryStatistics();

        long p95 = sorted.isEmpty() ? 0 : sorted.get((int) (sorted.size() * 0.95));
        long p99 = sorted.isEmpty() ? 0 : sorted.get((int) (sorted.size() * 0.99));

        String result = String.format("""
            ┌─────────────────────────────────────┐
            │ 목표: %4d msg/s                      │
            ├─────────────────────────────────────┤
            │ 처리량 (Throughput): %8.1f msg/s  │
            │ Latency avg:         %8.2f ms     │
            │ Latency min:         %8.2f ms     │
            │ Latency max:         %8.2f ms     │
            │ Latency p95:         %8.2f ms     │
            │ Latency p99:         %8.2f ms     │
            └─────────────────────────────────────┘""",
                rate,
                throughput,
                stats.getAverage() / 1_000_000.0,
                stats.getMin() / 1_000_000.0,
                stats.getMax() / 1_000_000.0,
                p95 / 1_000_000.0,
                p99 / 1_000_000.0
        );

        return result;
    }
}
