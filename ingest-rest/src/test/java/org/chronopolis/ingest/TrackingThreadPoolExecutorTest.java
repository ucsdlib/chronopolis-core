package org.chronopolis.ingest;

import org.chronopolis.rest.models.Bag;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
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
    public void testExceptionInRunnable() throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        TrackingThreadPoolExecutor<Bag> threadPoolExecutor = new TrackingThreadPoolExecutor<>(4,
            4,
            5,
            TimeUnit.MINUTES,
            new LinkedBlockingQueue<Runnable>());

        Bag testBag = new Bag("test-bag", "test-depositor");
        Class<Bag> bagClass = Bag.class;
        Field id = bagClass.getDeclaredField("id");
        id.setAccessible(true);
        id.set(testBag, 1L);

        threadPoolExecutor.submitIfAvailable(new ExceptionRunnable(), testBag);

        TimeUnit.SECONDS.sleep(2);

        Assert.assertFalse(threadPoolExecutor.contains(testBag));
    }

    @Test
    public void testRejected() throws NoSuchFieldException, IllegalAccessException {
        TrackingThreadPoolExecutor<Bag> threadPoolExecutor = new TrackingThreadPoolExecutor<>(1,
            1,
            5,
            TimeUnit.MINUTES,
            new ArrayBlockingQueue<Runnable>(1));

        Class<Bag> bagClass = Bag.class;
        Field id = bagClass.getDeclaredField("id");
        id.setAccessible(true);

        Bag testBag = new Bag("test-bag", "test-depositor");
        Bag testBag2 = new Bag("test-bag-2", "test-depositor");
        Bag testBag3 = new Bag("test-bag-3", "test-depositor");

        id.set(testBag, 1L);
        id.set(testBag2, 2L);
        id.set(testBag3, 3L);

        threadPoolExecutor.submitIfAvailable(new SleepyRunnable(), testBag);
        threadPoolExecutor.submitIfAvailable(new SleepyRunnable(), testBag2);
        threadPoolExecutor.submitIfAvailable(new SleepyRunnable(), testBag3);

        Assert.assertTrue(threadPoolExecutor.contains(testBag));
        Assert.assertTrue(threadPoolExecutor.contains(testBag2));
        Assert.assertFalse(threadPoolExecutor.contains(testBag3));

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