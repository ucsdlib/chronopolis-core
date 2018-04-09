package org.chronopolis.tokenize;

import com.google.common.collect.Queues;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of a StateMachine which tracks {@link ManifestEntry}s being processed and puts
 * them into a {@link BlockingQueue} depending on what processing step they are ready for.
 *
 * This implementation needs to be polled from in order for work to be done; otherwise it will block
 * indefinitely when trying to add a {@link ManifestEntry} to be worked on.
 * <p>
 * @author shake
 */
public class TokenStateMachine implements StateMachine {

    private final Logger log = LoggerFactory.getLogger(TokenStateMachine.class);

    private static final int MAX_QUEUE = 10000; // idk

    /**
     * Entries which are queued and ready to have tokens created
     */
    private final BlockingQueue<ManifestEntry> queued = new LinkedBlockingQueue<>(MAX_QUEUE);

    /**
     * Entries which have had tokens created and are ready to be registered to the Ingest Server
     */
    private final BlockingQueue<ManifestEntry> retrieved = new LinkedBlockingQueue<>(MAX_QUEUE);

    /**
     * All entries which have been submitted for processing
     */
    private final Set<ManifestEntry> processing = new ConcurrentSkipListSet<>();

    /**
     * Mapping of a {@link ManifestEntry} to a {@link TokenResponse} which was received from the
     * ACE IMS
     */
    private final Map<ManifestEntry, TokenResponse> tokens = new ConcurrentHashMap<>();
    private final Semaphore available = new Semaphore(MAX_QUEUE, true);


    @Override
    public void start(ManifestEntry entry) {
        // should return something off of this
        boolean interrupted = false;
        try {
            available.acquire();
            boolean add = processing.add(entry);
            if (add) {
                queued.put(entry);
            }
        } catch (InterruptedException e) {
            log.info("Thread interrupted!", e);
            interrupted = true;
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }

    }

    @Override
    public void retry(ManifestEntry entry) {
        boolean offer;
        if (processing.contains(entry)) {
            // at this point we should be guaranteed to have room available, but there's still
            // a chance of failure so capture the return
            offer = queued.offer(entry);

            if (!offer) {
                processing.remove(entry);
                tokens.remove(entry);
                available.release();
            }
        }
    }

    @Override
    public void associate(ManifestEntry entry, TokenResponse response) {
        boolean interrupted = false;
        if (processing.contains(entry)) {
            tokens.put(entry, response);
            try {
                retrieved.put(entry);
            } catch (InterruptedException e) {
                log.warn("Thread interrupted", e);
                complete(entry);
                interrupted = true;
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    @Override
    public void complete(ManifestEntry entry) {
        if (processing.contains(entry)) {
            processing.remove(entry);
            tokens.remove(entry);
            available.release();
        }
    }

    @Override
    public Set<ManifestEntry> queuedEntries(int size, long timeout, TimeUnit timeUnit) {
        Set<ManifestEntry> entries = new HashSet<>(size);
        Queues.drainUninterruptibly(queued, entries, size, timeout, timeUnit);
        return entries;
    }

    @Override
    public Map<ManifestEntry, TokenResponse> tokenizedEntries(int size, long timeout, TimeUnit timeUnit) {
        Set<ManifestEntry> entries = new HashSet<>(size);
        Queues.drainUninterruptibly(retrieved, entries, size, timeout, timeUnit);
        return entries.stream()
                .collect(Collectors.toMap((entry) -> entry, tokens::get));
    }
}
