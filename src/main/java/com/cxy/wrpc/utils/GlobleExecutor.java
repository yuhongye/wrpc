package com.cxy.wrpc.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
public class GlobleExecutor {
    private final static ThreadPoolExecutor executor;

    static {
        int processor = Runtime.getRuntime().availableProcessors();
        log.info("available processor: {}", processor);
        executor = new ThreadPoolExecutor(processor, processor * 2, 600L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(65536));
    }

    public static void submit(Runnable runnable) {
        executor.submit(runnable);
    }

    public static void stop() {
        executor.shutdown();
    }
}
