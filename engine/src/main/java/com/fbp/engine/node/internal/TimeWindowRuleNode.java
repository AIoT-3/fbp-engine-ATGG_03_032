package com.fbp.engine.node.internal;

import com.fbp.engine.core.node.AbstractNode;
import com.fbp.engine.message.Message;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

@Getter
public class TimeWindowRuleNode extends AbstractNode {
    private Predicate<Message> condition;
    private long windowMs;
    private int threshold;
    private final Queue<LocalDateTime> events = new ConcurrentLinkedQueue();

    public TimeWindowRuleNode(String id, Predicate<Message> condition, long windowMs, int threshold) {
        super(id);
        if(condition==null){
            throw new IllegalArgumentException("condition must be notNull");
        }
        if(windowMs<=0){
            throw new IllegalArgumentException("windowMs must be more than 0");
        }
        if(threshold<=0){
            throw new IllegalArgumentException("threshold must be more than 0");
        }
        this.condition = condition;
        this.windowMs = windowMs;
        this.threshold = threshold;

        addInputPort("in");
        addOutputPort("alert");
        addOutputPort("pass");
    }

    @Override
    public void onProcess(String portName, Message message) {
        if (message == null) return;

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = now.minusNanos(1_000_000L * windowMs);

        synchronized (events) {
            if (condition.test(message)) {
                events.add(now);
            }
            events.removeIf(t -> t.isBefore(windowStart));

            if (events.size() >= threshold) {
                send("alert", message);
            } else {
                send("pass", message);
            }
        }
    }
}
