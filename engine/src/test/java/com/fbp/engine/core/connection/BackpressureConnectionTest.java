package com.fbp.engine.core.connection;

import com.fbp.engine.core.connection.strategy.BlockStrategy;
import com.fbp.engine.core.connection.strategy.DropNewestStrategy;
import com.fbp.engine.core.connection.strategy.DropOldestStrategy;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BackpressureConnectionTest {

    @Test
    @DisplayName("Block 전략: 큐 가득 참 → send()가 블로킹됨")
    void testBlockStrategy() throws InterruptedException {
        Connection connection = new Connection("test-conn", 2);
        connection.setStrategy(new BlockStrategy());

        connection.deliver(new Message(Map.of("id", "msg1")));
        connection.deliver(new Message(Map.of("id", "msg2")));

        CountDownLatch latch = new CountDownLatch(1);
        Thread producer = new Thread(() -> {
            connection.deliver(new Message(Map.of("id", "msg3")));
            latch.countDown();
        });
        producer.start();

        boolean finished = latch.await(100, TimeUnit.MILLISECONDS);
        assertFalse(finished, "Producer should be blocked");

        Message polled = connection.poll();
        assertEquals("msg1", polled.get("id"));

        finished = latch.await(1, TimeUnit.SECONDS);
        assertTrue(finished, "Producer should finish after unblocking");
        
        assertEquals(2, connection.getBufferSize());
    }

    @Test
    @DisplayName("DropOldest 전략: 큐 가득 참 + 새 메시지 → 가장 오래된 메시지 제거")
    void testDropOldestStrategy() {
        Connection connection = new Connection("test-conn", 2);
        connection.setStrategy(new DropOldestStrategy());

        connection.deliver(new Message(Map.of("id", "msg1")));
        connection.deliver(new Message(Map.of("id", "msg2")));
        
        connection.deliver(new Message(Map.of("id", "msg3")));

        assertEquals(2, connection.getBufferSize());
        assertEquals("msg2", connection.poll().get("id"));
        assertEquals("msg3", connection.poll().get("id"));
        assertEquals(1, connection.getDroppedMessages());
    }

    @Test
    @DisplayName("DropNewest 전략: 큐 가득 참 + 새 메시지 → 새 메시지가 버려짐")
    void testDropNewestStrategy() {
        Connection connection = new Connection("test-conn", 2);
        connection.setStrategy(new DropNewestStrategy());

        connection.deliver(new Message(Map.of("id", "msg1")));
        connection.deliver(new Message(Map.of("id", "msg2")));
        
        connection.deliver(new Message(Map.of("id", "msg3")));

        assertEquals(2, connection.getBufferSize());
        assertEquals("msg1", connection.poll().get("id"));
        assertEquals("msg2", connection.poll().get("id"));
        
        assertEquals(0, connection.getBufferSize());
        assertEquals(1, connection.getDroppedMessages());
    }

    @Test
    @DisplayName("전략 변경: 런타임에 전략 변경 후 새 전략이 적용됨")
    void testStrategyChangeAtRuntime() {
        Connection connection = new Connection("test-conn", 2);
        connection.setStrategy(new DropNewestStrategy());

        connection.deliver(new Message(Map.of("id", "msg1")));
        connection.deliver(new Message(Map.of("id", "msg2")));
        connection.deliver(new Message(Map.of("id", "msg3")));

        assertEquals(1, connection.getDroppedMessages());

        connection.setStrategy(new DropOldestStrategy());
        connection.deliver(new Message(Map.of("id", "msg4")));

        assertEquals(2, connection.getDroppedMessages());
        assertEquals("msg2", connection.poll().get("id"));
        assertEquals("msg4", connection.poll().get("id"));
    }

    @Test
    @DisplayName("큐 크기 설정: 생성 시 지정한 큐 용량이 적용됨")
    void testQueueCapacity() {
        Connection connection = new Connection("test-conn", 5);
        connection.setStrategy(new DropNewestStrategy());

        for (int i = 0; i < 10; i++) {
            connection.deliver(new Message(Map.of("id", "msg" + i)));
        }

        assertEquals(5, connection.getBufferSize());
        assertEquals(5, connection.getDroppedMessages());
    }
}
