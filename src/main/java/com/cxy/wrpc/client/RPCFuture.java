package com.cxy.wrpc.client;

import com.cxy.wrpc.protocol.RpcRequest;
import com.cxy.wrpc.protocol.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class RPCFuture implements Future<Object> {
    private Sync sync;

    private RpcRequest request;
    private RpcResponse response;

    private long startTime;
    private long responseTimeThreshold = 5000;

    private List<RpcCallback> pendingCallbacks = new ArrayList<>();
    private ReentrantLock lock = new ReentrantLock();

    public RPCFuture(RpcRequest request) {
        this.sync = new Sync();
        this.request = request;
        this.startTime = System.currentTimeMillis();
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("cancel is not supported.");
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException("cancel is not supported.");
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(-1);
        return response != null ? response.getResult() : null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(-1, unit.toNanos(timeout));
        if (success) {
            return response != null ? response.getResult() : null;
        } else {
            throw new RuntimeException("Timeout exception. Request id: " + request.getRequestId() +
                    ". Request class name: " + request.getClassName() +
                    ". Request method: " + request.getMethodName());
        }
    }

    public void done(RpcResponse response) {
        this.response = response;
        sync.release(1);
        invokeDCallbacks();
        long timeCost = System.currentTimeMillis() - startTime;
        if (timeCost > responseTimeThreshold) {
            log.warn("Service response time is too low, Request id = {}, response time: {}MS", request.getRequestId(), timeCost);
        }
    }

    public RPCFuture addCallback(RpcCallback callback) {
        lock.lock();
        try {
            if (isDone()) {
                runCallback(callback);
            } else {
                this.pendingCallbacks.add(callback);
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    private void invokeDCallbacks() {
        lock.lock();
        try {
            pendingCallbacks.forEach(this::runCallback);
        } finally {
            lock.unlock();
        }
    }

    private void runCallback(final RpcCallback callback) {
        final RpcResponse res = this.response;
        RpcClient.submit(() -> {
            if (res.isOK()) {
                callback.success(res.getResult());
            } else {
                callback.fail(new RuntimeException(res.getError()));
            }
        });
    }

    static class Sync extends AbstractQueuedSynchronizer {
        static final int DONE = 1;
        static final int PENDING = 0;

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == DONE;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == PENDING) {
                if (compareAndSetState(PENDING, DONE)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        protected boolean isDone() {
            return getState() == DONE;
        }
    }
}
