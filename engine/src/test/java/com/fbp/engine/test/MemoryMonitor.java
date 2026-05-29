package com.fbp.engine.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MemoryMonitor {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final List<Long> memoryUsageHistory = new ArrayList<>();
    private final Runtime runtime = Runtime.getRuntime();

    public void start(long intervalMs) {
        scheduler.scheduleAtFixedRate(() -> {
            System.gc();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            synchronized (memoryUsageHistory) {
                memoryUsageHistory.add(usedMemory);
            }
        }, 0, intervalMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    public List<Long> getHistory() {
        synchronized (memoryUsageHistory) {
            return new ArrayList<>(memoryUsageHistory);
        }
    }

    public boolean isMonotonicallyIncreasing() {
        List<Long> history = getHistory();
        if (history.size() < 3) return false;

        int mid = history.size() / 2;
        long firstHalfSum = 0;
        for (int i = 0; i < mid; i++) firstHalfSum += history.get(i);
        long secondHalfSum = 0;
        for (int i = mid; i < history.size(); i++) secondHalfSum += history.get(i);

        double firstHalfAvg = (double) firstHalfSum / mid;
        double secondHalfAvg = (double) secondHalfSum / (history.size() - mid);

        return secondHalfAvg > firstHalfAvg * 1.2;
    }
}
