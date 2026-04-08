package com.fbp.engine.core.connection.impl;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.core.port.InputPort;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ConnectionTest {
    @Mock
    InputPort inputPort;

    Connection connection;

    @BeforeEach
    void setUp(){
        connection = new Connection("test");
        connection.setTarget(inputPort);
    }

    @Order(1)
    @Test
    @DisplayName("deliver-poll 기본 동작")
    void deliveredMessageCanPoll(){
        connection.deliver(new Message(
                Map.of("key","value")
        ));

        Assertions.assertNotNull(connection.poll());
    }

    @Order(2)
    @Test
    @DisplayName("메시지 순서 보장")
    void messageOrderFifo(){
        for(int i=0; i<5; i++) {
            connection.deliver(new Message(
                    Map.of("key" + i, "value" + i)
            ));
        }

        for(int i=0; i<5; i++){
            Assertions.assertTrue(connection.poll().toString().contains("value"+i));
        }
    }

    @Order(3)
    @Test
    @DisplayName("멀티스레드 deliver-poll")
    void multiThreadDeliverPoll() throws InterruptedException {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(2);
        final Message[] message = {null};

        Thread thread = new Thread(() -> {
            connection.deliver(new Message(Map.of("test", "value")));
            latch1.countDown();
            latch2.countDown();
        });

        Thread thread1 = new Thread(() -> {
            try {
                message[0] = connection.poll();
            }finally {
                latch2.countDown();
            }
        });

        thread.start();
        latch1.await();

        thread1.start();
        latch2.await();

        Assertions.assertNotNull(message[0]);
    }

    @Order(4)
    @Test
    @DisplayName("poll 대기 동작")
    void pollWaiting(){
        final Message[] message = {null};
        CountDownLatch countDownLatch1 = new CountDownLatch(1);
        CountDownLatch countDownLatch = new CountDownLatch(2);
        Message expectedMessage = new Message(java.util.Map.of("test", "blocking"));

        Thread consumer = new Thread(
                () -> {
                    try {
                        countDownLatch1.await();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    message[0] = connection.poll();
                    countDownLatch.countDown();
                }
        );
        consumer.start();
        Assertions.assertNull(message[0]);

        Thread provider = new Thread(
                () ->{
                    connection.deliver(expectedMessage);
                    countDownLatch1.countDown();
                    countDownLatch.countDown();
                }
        );
        provider.start();

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertEquals(expectedMessage, message[0]);
    }

    @Order(5)
    @Test
    @DisplayName("버퍼 크기 제한")
    void bufferLimit(){
        Connection testConn = new Connection("tc", 2);

        Thread thread = new Thread(
                () ->{
                    for(int i=0; i<3; i++){
                        testConn.deliver(new Message(
                                Map.of("key", "value")
                                )
                        );
                    }
                }
        );
        thread.start();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Assertions.assertEquals(Thread.State.WAITING, thread.getState());
    }

    @Order(6)
    @Test
    @DisplayName("버퍼 크기 조회")
    void bufferSizeCheck(){
        Thread thread = new Thread(
                () ->{
                    for(int i=0; i<10; i++){
                        connection.deliver(new Message(
                                        Map.of("key", "value")
                                )
                        );
                    }
                }
        );
        thread.start();

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Assertions.assertEquals(10, connection.getBufferSize());
    }

}
