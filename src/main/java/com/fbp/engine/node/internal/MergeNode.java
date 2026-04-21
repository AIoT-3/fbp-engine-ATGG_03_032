package com.fbp.engine.node.internal;

import com.fbp.engine.message.Message;
import com.fbp.engine.core.node.AbstractNode;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MergeNode extends AbstractNode {
    private Queue<Message> pending1Queue;
    private Queue<Message> pending2Queue;

    public MergeNode(String id) {
        super(id);

        this.pending1Queue = new ConcurrentLinkedQueue<>();
        this.pending2Queue = new ConcurrentLinkedQueue<>();

        addInputPort("in-1");
        addInputPort("in-2");
        addOutputPort("out");
    }

    @Override
    public void onProcess(String portName, Message message) {
        if (message == null) {
            throw new IllegalArgumentException("message must be notNull");
        }

        if (portName.equals("in-1")) {
            pending1Queue.add(message);
        } else if (portName.equals("in-2")) {
            pending2Queue.add(message);
        } else {
            throw new IllegalArgumentException("came in portName must be 'in-1' or 'in-2'");
        }

        synchronized (this) {
            if (!pending1Queue.isEmpty() && !pending2Queue.isEmpty()) {
                Message pending1 = pending1Queue.poll();
                Message pending2 = pending2Queue.poll();

                if (pending1 != null && pending2 != null) {
                    Map<String, Object> mergedPayload = new HashMap<>(pending1.getPayload());
                    mergedPayload.putAll(pending2.getPayload());

                    send("out", new Message(mergedPayload));
                }
            }
        }
    }
}
