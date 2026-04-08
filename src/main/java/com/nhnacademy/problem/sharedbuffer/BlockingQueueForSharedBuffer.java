package com.nhnacademy.problem.sharedbuffer;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
public class BlockingQueueForSharedBuffer {
    private BlockingQueue<String> buffer = new LinkedBlockingQueue<>();

    void provide(){
        for(int i=0; i<100; i++){
            try {
                buffer.put(String.format("message-%d", i));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            buffer.put("END");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    void consume() {
        while (true) {
            String value = null;
            try {
                value = buffer.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            log.info(value);
            if (value.equals("END")) {
                return;
            }
        }
    }

    public static void main(String[] args){
        BlockingQueueForSharedBuffer blockingQueueForSharedBuffer = new BlockingQueueForSharedBuffer();

        Thread provider = new Thread(() -> {
            blockingQueueForSharedBuffer.provide();
        });
        Thread consumer = new Thread(() ->{
            blockingQueueForSharedBuffer.consume();
        });

        provider.start();
        consumer.start();
    }
}
