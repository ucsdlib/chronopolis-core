package org.chronopolis.common.concurrent;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Test some possible error states which can come up when submitting to a TrackingThreadPool
 *
 * Created by shake on 3/17/16.
 */
public class TrackingThreadPoolExecutorTest {

    @Test
    public void testExceptionInRunnable() throws InterruptedException {
        TrackingThreadPoolExecutor<Integer> threadPoolExecutor = new TrackingThreadPoolExecutor<>(4,
            4,
            5,
            TimeUnit.MINUTES,
                new LinkedBlockingQueue<>());

        Integer i = 1;

        threadPoolExecutor.submitIfAvailable(new ExceptionRunnable(), i);

        TimeUnit.SECONDS.sleep(2);

        Assert.assertFalse(threadPoolExecutor.contains(i));
    }

    @Test
    public void testRejected() {
        TrackingThreadPoolExecutor<Integer> threadPoolExecutor = new TrackingThreadPoolExecutor<>(1,
            1,
            5,
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<>(1));

        Integer one = 1;
        Integer two = 2;
        Integer three = 3;

        threadPoolExecutor.submitIfAvailable(new SleepyRunnable(), one);
        threadPoolExecutor.submitIfAvailable(new SleepyRunnable(), two);
        threadPoolExecutor.submitIfAvailable(new SleepyRunnable(), three);

        Assert.assertTrue(threadPoolExecutor.contains(one));
        Assert.assertTrue(threadPoolExecutor.contains(two));
        Assert.assertFalse(threadPoolExecutor.contains(three));

        threadPoolExecutor.shutdown();
    }

    private class SleepyRunnable implements Runnable {
        @Override
        public void run() {
            try {
                TimeUnit.MILLISECONDS.sleep(500);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private class ExceptionRunnable implements Runnable {

        @Override
        public void run() {
            // System.out.println("hello");
            throw new RuntimeException("Exception in run");
        }
    }

}