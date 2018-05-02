package org.chronopolis.common.concurrent;

import com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Thread pool which keeps track of a mutable object T throughout its execution lifetime
 * Upon completion it can be resubmitted to the pool with a new task
 * <p/>
 * TODO: Expose some of this to admin users, just in case?
 * <p/>
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
        setRejectedExecutionHandler(new RejectedHandler());
    }

    public Optional<FutureTask<T>> submitIfAvailable(Runnable r, T item) {
        if (items.add(item)) {
            log.trace("Adding {}", item.toString());
            FutureTask<T> t = new Task(r, item);
            execute(t);
            return Optional.of(t);
        }

        log.trace("Rejected {}", item);
        return Optional.empty();
    }

    public void destroy() {
        for (Runnable runnable : super.getQueue()) {
            if (runnable instanceof FutureTask) {
                FutureTask f = (FutureTask) runnable;
                f.cancel(true);
            }
        }

        shutdownNow();
    }

    @VisibleForTesting
    public boolean contains(T item) {
        return items.contains(item);
    }

    public class RejectedHandler implements RejectedExecutionHandler {

        @Override
        public void rejectedExecution(Runnable runnable, ThreadPoolExecutor threadPoolExecutor) {
            if (runnable instanceof TrackingThreadPoolExecutor.Task) {
                Task task = (Task) runnable;
                T result = task.result;
                log.warn("Task was rejected: {}", result.toString());
                items.remove(result);
            }
        }
    }


    public class Task extends FutureTask<T> {

        // We want to be able to use this without super.call
        private T result;

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
