package org.chronopolis.ingest;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Thread pool which keeps track of a mutable object T throughout its execution lifetime
 * Upon completion it can be resubmitted to the pool with a new task
 *
 * Created by shake on 2/3/16.
 */
public class TrackingThreadPoolExecutor<T> extends ThreadPoolExecutor {
    private final Logger log = LoggerFactory.getLogger(TrackingThreadPoolExecutor.class);

    // List of items we're working on
    private Set<T> items = new ConcurrentSkipListSet<>();

    public TrackingThreadPoolExecutor(int corePoolSize,
                                      int maximumPoolSize,
                                      long keepAliveTime,
                                      TimeUnit unit,
                                      BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public Optional<FutureTask<T>> submitIfAvailable(Runnable r, T item) {
        if (items.add(item)) {
            log.debug("Adding {}", item.toString());
            FutureTask<T> t = new Task(r, item);
            execute(t);
            return Optional.of(t);
        }

        log.debug("Rejected {}", item);
        return Optional.absent();
    }

    public void destroy() {
        for (Runnable runnable : super.getQueue()) {
            if (runnable instanceof FutureTask) {
                FutureTask f = (FutureTask) runnable;
                f.cancel(true);
            }
        }

        shutdown();
        shutdownNow();
    }

    @VisibleForTesting
    public boolean contains(T item) {
        return items.contains(item);
    }

    public class Task extends FutureTask<T> {

        // We want to be able to use this without super.call
        T result;

        public Task(Runnable runnable, T result) {
            super(runnable, result);
            this.result = result;
        }

        public void run() {
            super.run();
            log.debug("Removing {}", result.toString());
            items.remove(result);
        }
    }

}
