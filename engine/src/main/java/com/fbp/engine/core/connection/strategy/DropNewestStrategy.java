package com.fbp.engine.core.connection.strategy;

import com.fbp.engine.message.Message;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class DropNewestStrategy implements BackpressureStrategy {
    @Override
    public boolean offer(Message message, BlockingQueue<Message> queue, AtomicLong dropCounter) {
        if (queue.offer(message)) {
            return true;
        } else {
            dropCounter.incrementAndGet();
            return false;
        }
    }
}
