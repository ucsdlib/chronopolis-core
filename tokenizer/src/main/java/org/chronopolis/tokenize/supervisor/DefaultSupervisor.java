package org.chronopolis.tokenize.supervisor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Queues;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.tokenize.ManifestEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of a Supervisor which tracks {@link ManifestEntry}s being processed and puts
 * them into a {@link BlockingQueue} depending on what processing step they are ready for.
 * <p>
 * This implementation needs to be polled from in order for work to be done; otherwise it will block
 * indefinitely when trying to add a {@link ManifestEntry} to be worked on.
 * <p>
 *
 * @author shake
 */
public class DefaultSupervisor implements TokenWorkSupervisor {

    private final Logger log = LoggerFactory.getLogger(DefaultSupervisor.class);

    private static final int MAX_QUEUE = 10000; // idk
    private final Semaphore available = new Semaphore(MAX_QUEUE, true);

    /**
     * Entries which are queued and ready to have tokens created
     */
    private final BlockingQueue<ManifestEntry> queued = new LinkedBlockingQueue<>(MAX_QUEUE);

    /**
     * Entries which have had tokens created and are ready to be registered to the Ingest Server
     */
    private final BlockingQueue<ManifestEntry> retrieved = new LinkedBlockingQueue<>(MAX_QUEUE);

    /**
     * Map to track ManifestEntries which have been submitted and their current State
     */
    private final Map<ManifestEntry, WorkUnit> processing = new ConcurrentSkipListMap<>();

    /**
     * Add a {@link ManifestEntry} to the queue for processing. This blocks until space is available
     * for processing.
     * <p>
     * Preconditions:
     * - Then entry is not already being processed
     * <p>
     * Postconditions:
     * - The ManifestEntry state == QUEUED_FOR_TOKENIZATION
     *
     * @param entry the ManifestEntry to add
     * @return true if the ManifestEntry was added to the queue, false otherwise
     */
    @Override
    public boolean start(ManifestEntry entry) {
        boolean add = false;
        boolean interrupted = false;
        try {
            available.acquire();
            WorkUnit put = processing.put(entry, new WorkUnit());

            // basically the above returns null iff no mapping exists
            // so to avoid adding something twice, make sure that is true
            if (put == null) {
                queued.put(entry);
                add = true;
            } else {
                // If we already added the ManifestEntry, release the semaphore we just acquired
                available.release();
            }
        } catch (InterruptedException e) {
            log.info("Thread interrupted!", e);
            interrupted = true;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }

        return add;
    }

    /**
     * Attempt to add a ManifestEntry to the queue for token creation. This is non-blocking
     * so if the ManifestEntry cannot be added to the queue it is purged from tracking.
     * <p>
     * No acquisition needs to be done from the semaphore assuming that the ManifestEntry has
     * previously been submitted for processing. If it has not been submitted previously, no attempt
     * to requeue the given ManifestEntry will be made.
     * <p>
     * This is NOT thread safe.
     * <p>
     * Preconditions:
     * - The ManifestEntry has previously been submitted
     * - The ManifestEntry has state == REQUESTING_TOKEN
     *
     * @param entry the ManifestEntry being resubmitted
     * @return true if the ManifestEntry was queued; false otherwise
     */
    @Override
    public boolean retryTokenize(ManifestEntry entry) {
        boolean offer = false;
        log.debug("[{}] Attempting to requeue for tokenization", entry.getPath());
        WorkUnit enc = processing.get(entry);
        if (enc != null && enc.getState() == WorkUnit.State.REQUESTING_TOKEN) {
            // at this point we should be guaranteed to have room available, but there's still
            // a chance of failure so capture the return
            offer = queued.offer(entry);

            if (!offer) {
                log.warn("[{}] Failed to requeue. Removing from supervisor.", entry.getPath());
                rmAll(entry);
            }
        }

        return offer;
    }

    /**
     * Attempt to add a ManifestEntry to the queue for registering ACE Tokens. This is non-blocking
     * so if the ManifestEntry cannot be added to the queue it is purged from tracking.
     * <p>
     * No acquisition needs to be done from the semaphore assuming that the ManifestEntry has
     * previously been submitted for processing. If it has not been submitted previously, no attempt
     * to requeue the given ManifestEntry will be made.
     * <p>
     * This is NOT thread safe.
     * <p>
     * Preconditions:
     * - The ManifestEntry has previously been submitted
     * - The ManifestEntry has an associated TokenResponse
     * - The ManifestEntry state == REGISTERING_TOKEN
     *
     * @param entry the ManifestEntry being resubmitted
     * @return true if the ManifestEntry was queued; false otherwise
     */
    @Override
    public boolean retryRegister(ManifestEntry entry) {
        log.debug("[{}] Attempting to requeue for registration", entry.getPath());
        boolean offer = false;
        WorkUnit enc = processing.get(entry);
        if (enc != null && enc.getResponse() != null
                && enc.getState() == WorkUnit.State.REGISTERING_TOKEN) {
            offer = retrieved.offer(entry);

            if (!offer) {
                log.warn("[{}] Failed to requeue. Removing from supervisor.", entry.getPath());
                rmAll(entry);
            }
        }

        return offer;
    }

    /**
     * Associate a {@link ManifestEntry} with a {@link TokenResponse} which was received by the ACE
     * IMS. Upon successful association, add the ManifestEntry to the queue for registering ACE
     * Tokens with the Chronopolis Ingest Server.
     * <p>
     * This is a blocking operation.
     * <p>
     * Preconditions:
     * - The ManifestEntry has previously been submitted
     * - The ManifestEntry does not have a TokenResponse already associated (subject to change)
     * - The ManifestEntry state == REQUESTING_TOKEN
     * <p>
     * Postconditions:
     * - The ManifestEntry state == QUEUED_FOR_REGISTRATION
     *
     * @param entry    the ManifestEntry which was successfully tokenized
     * @param response the TokenResponse being associated
     */
    @Override
    public boolean associate(ManifestEntry entry, TokenResponse response) {
        boolean success = false;
        boolean interrupted = false;

        WorkUnit enc = processing.get(entry);

        if (enc != null && enc.getResponse() == null
                && enc.getState() == WorkUnit.State.REQUESTING_TOKEN) {
            enc.setResponse(response);
            enc.setState(WorkUnit.State.QUEUED_FOR_REGISTRATION);
            try {
                retrieved.put(entry);
                success = true;
            } catch (InterruptedException e) {
                log.warn("Interrupted while associating token", e);
                rmAll(entry);
                interrupted = true;
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return success;
    }

    /**
     * Complete the flow for a {@link ManifestEntry}. The entry is removed from all queues and maps
     * and any associated data.
     * <p>
     * Preconditions:
     * - ManifestEntry has previously been submitted
     * - ManifestEntry state == REGISTERING_TOKEN
     *
     * @param entry the ManifestEntry which has completed its lifecycle
     */
    @Override
    public void complete(ManifestEntry entry) {
        WorkUnit enc = processing.get(entry);
        if (enc != null) {
            enc.setResponse(null);
            processing.remove(entry, enc);
            available.release();
        }
    }

    /**
     * Retrieve {@link ManifestEntry}s which are queued for tokenization.
     * <p>
     * Postconditions:
     * - Each ManifestEntry state == REQUESTING_TOKEN
     *
     * @param size     the maximum amount of entries to get
     * @param timeout  the length of the timeout to wait for
     * @param timeUnit the unit of time to wait
     * @return the ManifestEntries
     */
    @Override
    public Set<ManifestEntry> queuedEntries(int size, long timeout, TimeUnit timeUnit) {
        Set<ManifestEntry> entries = new HashSet<>(size);

        // could we combine the drain/forEach?
        // we would need to write it ourselves.. but
        Queues.drainUninterruptibly(queued, entries, size, timeout, timeUnit);
        entries.forEach(entry -> processing.get(entry).setState(WorkUnit.State.REQUESTING_TOKEN));
        return entries;
    }

    /**
     * Retrieve {@link ManifestEntry}s which have an associated {@link TokenResponse} and are ready
     * to be registered with the Chronopolis Ingest Service.
     * <p>
     * Postconditions:
     * - Each ManifestEntry state == REGISTERING_TOKEN
     *
     * @param size     the maximum amount of entries to get
     * @param timeout  the length of the timeout to wait for
     * @param timeUnit the unit of time to wait
     * @return a Map of each available ManifestEntry with its TokenResponse
     */
    @Override
    public Map<ManifestEntry, TokenResponse> tokenizedEntries(int size,
                                                              long timeout,
                                                              TimeUnit timeUnit) {
        Set<ManifestEntry> entries = new HashSet<>(size);
        Queues.drainUninterruptibly(retrieved, entries, size, timeout, timeUnit);
        return entries.stream()
                .peek((entry) -> processing.get(entry).setState(WorkUnit.State.REGISTERING_TOKEN))
                .collect(Collectors.toMap(
                        (entry) -> entry,
                        (entry) -> processing.get(entry).getResponse()));
    }

    @Override
    public boolean isProcessing() {
        return processing.isEmpty();
    }

    @Override
    public boolean isProcessing(ManifestEntry entry) {
        return entry != null && processing.containsKey(entry);
    }

    private void rmAll(ManifestEntry entry) {
        WorkUnit enc = processing.remove(entry);
        if (enc != null) {
            enc.setResponse(null);
            available.release();
        }
    }

    @VisibleForTesting
    protected Map<ManifestEntry, WorkUnit> getProcessing() {
        return processing;
    }

    @VisibleForTesting
    int availablePermits() {
        return available.availablePermits();
    }

}
