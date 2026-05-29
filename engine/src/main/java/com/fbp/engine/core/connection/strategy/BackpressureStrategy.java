package com.fbp.engine.core.connection.strategy;

import com.fbp.engine.message.Message;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public interface BackpressureStrategy {
    /**
     * Attempts to offer a message to the queue according to the strategy.
     *
     * @param message the message to offer
     * @param queue the queue to offer the message to
     * @param dropCounter a counter for dropped messages
     * @return true if the message was added, false otherwise
     * @throws InterruptedException if interrupted while waiting
     */
    boolean offer(Message message, BlockingQueue<Message> queue, AtomicLong dropCounter) throws InterruptedException;
}
