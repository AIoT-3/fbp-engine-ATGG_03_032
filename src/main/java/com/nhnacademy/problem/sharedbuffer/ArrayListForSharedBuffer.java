package com.nhnacademy.problem.sharedbuffer;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class ArrayListForSharedBuffer {
    private volatile ArrayList<String> buffer = new ArrayList<>();
    private volatile boolean running = true;

    void provide(){
        for(int i=0; i<1000; i++){
            String str = "message-"+i;
            log.info(str);
            buffer.add(str);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        running = false;
    }

    void consume(){
        while(!buffer.isEmpty() || running){
            if(buffer.isEmpty()){
                continue;
            }
            log.info(buffer.remove(0));
        }
    }

    public static void main(String[] args){
        ArrayListForSharedBuffer arrayListForSharedBuffer = new ArrayListForSharedBuffer();
        Thread provider = new Thread(() -> {
            arrayListForSharedBuffer.provide();
        });
        provider.setName("provider-1");

        Thread consumer = new Thread(() ->{
            arrayListForSharedBuffer.consume();
        });
        consumer.setName("consumer-1");

        Thread consumer2 = new Thread(() -> {
            arrayListForSharedBuffer.consume();
        });
        consumer2.setName("consumer-2");

        provider.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        consumer.start();
        consumer2.start();

    }
}
