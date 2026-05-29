package com.fbp.engine.core.connection.strategy;

import com.fbp.engine.message.Message;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class BlockStrategy implements BackpressureStrategy {
    @Override
    public boolean offer(Message message, BlockingQueue<Message> queue, AtomicLong dropCounter) throws InterruptedException {
        queue.put(message);
        return true;
    }
}
