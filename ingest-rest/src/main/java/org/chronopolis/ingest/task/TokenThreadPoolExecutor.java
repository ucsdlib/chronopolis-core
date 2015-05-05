package org.chronopolis.ingest.task;

import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.models.Bag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Our own thread pool executor which keeps track of the Bags it currently has in queue.
 * This is done so that we don't attempt to tokenize bags multiple times
 *
 *
 * Created by shake on 2/27/15.
 */
public class TokenThreadPoolExecutor extends ThreadPoolExecutor {
    private final Logger log = LoggerFactory.getLogger(TokenThreadPoolExecutor.class);

    private Set<Bag> workingBags = new ConcurrentSkipListSet<>();

    public TokenThreadPoolExecutor(int corePoolSize,
                                   int maximumPoolSize,
                                   long keepAliveTime,
                                   TimeUnit unit,
                                   BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    /**
     * Submit a bag for tokenization. If the bag has already been submitted, or
     * there are any errors when submitting, return false
     *
     * TODO: Is this the best name for the method?
     *
     * @param b               - the bag to submit
     * @param settings        - the settings for the ingest service
     * @param bagRepository   - the repository which holds the bags
     * @param tokenRepository - the repository which holds the tokens
     * @return
     */
    public boolean submitBagIfAvailable(Bag b,
                                        IngestSettings settings,
                                        BagRepository bagRepository,
                                        TokenRepository tokenRepository) {
        boolean submitted = true;

        // Try to add the bag to the set
        if (workingBags.add(b)) {
            TokenRunner tr = new TokenRunner(b,
                    settings.getBagStage(),
                    settings.getTokenStage(),
                    bagRepository,
                    tokenRepository);

            // submit the runnable with the bag
            // this ensures we can retrieve the bag in afterExecute
            try {
                this.submit(tr, b);
            } catch (NullPointerException e) {
                log.error("Cannot submit null bag!", e);
                submitted = false;
            } catch (RejectedExecutionException e) {
                log.error("Bag {} rejected!", b.getID(), e);
                submitted = false;
            }

        } else {
            log.debug("Bag {} already submitted, skipping", b.getName());
            submitted = false;
        }


        return submitted;
    }

    /**
     * Method to remove the bag from our working set, if r is an instance of a TokenRunner
     *
     * @param r
     * @param t
     */
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        log.debug("After execute");

        if (r instanceof FutureTask) {
            // Remove the bag from our working set
            FutureTask<Bag> task = (FutureTask<Bag>) r;
            try {
                Bag b = task.get();
                boolean success = workingBags.remove(b);
                /*
                if (success == false) {
                    log.debug("Bag is null?: {}", b == null);
                    log.debug("Bag Info: {} {} {} {} {} {} {}", new Object[]{
                            b.getSize(),
                            b.getTotalFiles(),
                            b.getID(),
                            b.getDepositor(),
                            b.getFixityAlgorithm(),
                            b.getLocation(),
                            b.getName()
                    });

                    boolean contains = workingBags.contains(b);

                    // submit a busy task and try to remove the bag again
                    if (contains) {
                        log.debug("Submitting busy task");
                        this.submit(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    TimeUnit.SECONDS.sleep(5);
                                } catch (InterruptedException e) {
                                }
                            }
                        }, b);
                    }
                }
                */
                log.debug("Removal of {} from the working set: {}", b.getName(), success);
            } catch (InterruptedException e) {
                log.error("Interrupted in afterExecute", e);
            } catch (ExecutionException e) {
                log.error("Execution error in afterExecute", e);
            }
        }

        // if there was an exception... we'll need to figure out what to do
        if (t != null) {
            log.error("Error while tokenizing bag:", t);
        }

        log.debug("Ending");
    }

}
