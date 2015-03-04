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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
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

            // submit the runnable
            try {
                this.submit(tr);
            } catch (NullPointerException e) {
                log.error("Cannot submit null bag!", e);
                submitted = false;
            } catch (RejectedExecutionException e) {
                log.error("Bag {} rejected!", b.getID(), e);
                submitted = false;
            }

        } else {
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

        if (r instanceof TokenRunner) {
            // remove the bag from our working set
            TokenRunner tr = (TokenRunner) r;
            workingBags.remove(tr.getBag());
        }

        // if there was an exception... we'll need to figure out what to do
        if (t != null) {
            log.error("Error while tokenizing bag:", t);
        }

    }

}
