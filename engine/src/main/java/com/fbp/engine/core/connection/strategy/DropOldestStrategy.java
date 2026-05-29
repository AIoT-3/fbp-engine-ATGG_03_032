package com.fbp.engine.core.connection.strategy;

import com.fbp.engine.message.Message;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class DropOldestStrategy implements BackpressureStrategy {
    @Override
    public boolean offer(Message message, BlockingQueue<Message> queue, AtomicLong dropCounter) {
        if (!queue.offer(message)) {
            queue.poll();
            dropCounter.incrementAndGet();
            if (!queue.offer(message)) {
                return false;
            }
        }
        return true;
    }
}
