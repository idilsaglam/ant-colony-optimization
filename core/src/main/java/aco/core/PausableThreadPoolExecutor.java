/*
22015094 - SAGLAM Idil
*/
package aco.core;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class PausableThreadPoolExecutor extends ThreadPoolExecutor {
    private boolean isPaused;
    private ReentrantLock pauseLock = new ReentrantLock();
    private Condition unpaused = pauseLock.newCondition();

    public PausableThreadPoolExecutor(int corePoolSIze, int maxPoolSize) {
        this(corePoolSIze, maxPoolSize, Long.MAX_VALUE, TimeUnit.DAYS, new LinkedBlockingDeque<>());
    }

    /**
     * Creates a new {@code ThreadPoolExecutor} with the given initial parameters, the {@linkplain
     * Executors#defaultThreadFactory default thread factory} and the {@linkplain AbortPolicy
     * default rejected execution handler}.
     *
     * <p>It may be more convenient to use one of the {@link Executors} factory methods instead of
     * this general purpose constructor.
     *
     * @param corePoolSize the number of threads to keep in the pool, even if they are idle, unless
     *     {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the pool
     * @param keepAliveTime when the number of threads is greater than the core, this is the maximum
     *     time that excess idle threads will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @param workQueue the queue to use for holding tasks before they are executed. This queue will
     *     hold only the {@code Runnable} tasks submitted by the {@code execute} method.
     * @throws IllegalArgumentException if one of the following holds:<br>
     *     {@code corePoolSize < 0}<br>
     *     {@code keepAliveTime < 0}<br>
     *     {@code maximumPoolSize <= 0}<br>
     *     {@code maximumPoolSize < corePoolSize}
     * @throws NullPointerException if {@code workQueue} is null
     */
    public PausableThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        pauseLock.lock();
        try {
            while (isPaused) {
                unpaused.await();
            }
        } catch (InterruptedException e) {
            t.interrupt();
        } finally {
            pauseLock.unlock();
        }
    }

    public void pause() {
        pauseLock.lock();
        try {
            isPaused = true;
        } finally {
            pauseLock.unlock();
        }
    }

    public void resume() {
        pauseLock.lock();
        try {
            isPaused = false;
            unpaused.signal();
        } finally {
            pauseLock.unlock();
        }
    }

    private static final class TheadPoolQueue extends ArrayBlockingQueue<Runnable> {

        public TheadPoolQueue(int capacity) {
            super(capacity);
        }

        @Override
        public boolean offer(Runnable runnable) {
            try {
                put(runnable);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            return true;
        }
    }
}
