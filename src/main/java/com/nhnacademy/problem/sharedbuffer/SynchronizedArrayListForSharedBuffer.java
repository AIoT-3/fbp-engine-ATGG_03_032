package com.nhnacademy.problem.sharedbuffer;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class SynchronizedArrayListForSharedBuffer {
    private ArrayList<String> buffer = new ArrayList<>();

    void provide(){
        for(int i=0; i<100; i++){
            synchronized (buffer) {
                buffer.add(String.format("message-%d", i));
                buffer.notifyAll();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        synchronized (buffer) {
            buffer.add("END");
            buffer.notifyAll();
        }
    }

    void consume() {
        while (true) {
            String value;

            synchronized (buffer) {
                if (buffer.isEmpty()) {
                    try {
                        buffer.wait();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                value = buffer.remove(0);
            }

            log.info(value);
            if (value.equals("END")) {
                return;
            }
        }
    }

    public static void main(String[] args){
        SynchronizedArrayListForSharedBuffer synchronizedArrayListForSharedBuffer = new SynchronizedArrayListForSharedBuffer();
        Thread provider = new Thread(() -> {
            synchronizedArrayListForSharedBuffer.provide();
        });
        Thread consumer = new Thread(() ->{
            synchronizedArrayListForSharedBuffer.consume();
        });

        provider.start();
        consumer.start();

        try {
            provider.join();
            consumer.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
