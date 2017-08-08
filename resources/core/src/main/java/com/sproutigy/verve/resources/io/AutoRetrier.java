package com.sproutigy.verve.resources.io;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

public class AutoRetrier {
    private int maxRetries;
    private long timeoutDurationMillis;

    public AutoRetrier() {
        this(60, TimeUnit.SECONDS);
    }

    public AutoRetrier(long timeoutDuration, TimeUnit unit) {
        this(0, timeoutDuration, unit);
    }

    public AutoRetrier(int maxRetries) {
        this(maxRetries, 0, TimeUnit.MILLISECONDS);
    }

    public AutoRetrier(int maxRetries, long timeoutDuration, TimeUnit unit) {
        this.maxRetries = maxRetries;
        this.timeoutDurationMillis = unit.toMillis(timeoutDuration);
    }

    public void run(Runnable runnable) throws InterruptedException {
        Callable<Void> callable = new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                runnable.run();
                return null;
            }
        };
        try {
            call(callable);
        } catch (Throwable e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e;
            }
            if (e instanceof Error) {
                throw (Error)e;
            }
            throw new RuntimeException(e);
        }
    }

    public <V> V call(Callable<V> callable) throws Exception {
        int retries = 0;
        long sleepTimeMillis = 1;
        long started = System.currentTimeMillis();

        do {
            try {
                return callable.call();
            } catch (Throwable e) {
                boolean timeout = timeoutDurationMillis > 0 && (started + timeoutDurationMillis < System.currentTimeMillis());
                boolean tooManyRetries = maxRetries > 0 && retries >= maxRetries;
                if (timeout || tooManyRetries || !shouldRetry(e)) {
                    throw e;
                }

                Thread.sleep(sleepTimeMillis);
                sleepTimeMillis *= 2;
            }
        } while (true);
    }

    public boolean shouldRetry(Throwable e) {
        return (e instanceof Exception);
    }
}
