package org.chronopolis.tokenize.batch;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Queues;
import edu.umiacs.ace.ims.api.IMSService;
import edu.umiacs.ace.ims.api.TokenRequestBatch;
import edu.umiacs.ace.ims.ws.TokenRequest;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.TokenRegistrar;
import org.chronopolis.tokenize.config.TokenTaskConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Entry point for TokenRequest creation. This keeps a mapping of which token request
 * belongs to which Bag so that it can be later updated to the Ingest Server for Chronopolis.
 * <p>
 * Based off of {@link edu.umiacs.ace.ims.api.ImmediateTokenRequestBatch}. Different in that
 * instead of running the request threads itself, this creates {@link Runnable} objects which
 * are a single set of requests.
 *
 * @author shake
 */
public class ChronopolisTokenRequestBatch implements TokenRequestBatch, Runnable {

    private final Logger log = LoggerFactory.getLogger(TokenTaskConfiguration.TOKENIZER_LOG_NAME);

    private final AtomicBoolean running;
    private final LinkedBlockingQueue<TokenRequest> requests;
    private final ConcurrentHashMap<String, ManifestEntry> entries;
    private final List<TokenRequest> buffer;

    private final TokenService tokens;
    private final String tokenClass;
    private final int maxWaitTime;
    private final int maxQueueLength;
    private final ImsServiceWrapper ims;

    public ChronopolisTokenRequestBatch(AceConfiguration configuration, TokenService tokens) {
        this(configuration, tokens, new ImsServiceWrapper(configuration.getIms()));
    }

    public ChronopolisTokenRequestBatch(AceConfiguration configuration, TokenService tokens, ImsServiceWrapper wrapper) {
        this.ims = wrapper;
        this.tokens = tokens;

        AceConfiguration.Ims imsConfiguration = configuration.getIms();
        tokenClass = imsConfiguration.getTokenClass();
        maxWaitTime = imsConfiguration.getWaitTime();
        maxQueueLength = imsConfiguration.getQueueLength();

        running = new AtomicBoolean(true);
        entries = new ConcurrentHashMap<>();
        buffer = new ArrayList<>(maxQueueLength);
        requests = new LinkedBlockingQueue<>(imsConfiguration.getQueueLength() * 2);
    }

    /**
     * Add an entry found in a manifest to the most recent Batch Request
     * <p>
     * If an entry has already been added to the queue, it will not be resubmitted
     *
     * @param entry the entry to add
     */
    public boolean add(ManifestEntry entry) {
        log.debug("Adding request {}", entry.getPath());
        boolean offer = false;

        // we can't really avoid the scenario where
        // t1: running = true
        // t1: entries.put
        // t2: close
        // t1: requests.offer
        // unless we lock but maybe that's not a big deal since it will never be processed
        // anyway still some thinking to do on this section
        if (running.get()) {
            ManifestEntry put = entries.put(entry.tokenName(), entry);
            if (put == null) {
                offer = requests.offer(createRequest(entry));
            }
        } else {
            log.warn("Tokenizer has shutdown, unable to add {}", entry.getPath());
        }

        return offer;
    }

    /**
     * Not implemented in the ChronopolisTokenRequestBatch. Will throw a RuntimeException if called.
     *
     * @param tokenRequest
     * @throws InterruptedException
     */
    @Override
    public void add(TokenRequest tokenRequest) throws InterruptedException {
        throw new RuntimeException("Invalid Usage: add(TokenRequest) called for ChronopolisTokenRequestBatch");
    }

    /**
     * Notify a ChronopolisTokenRequestBatch that it should shutdown. Upon shutdown, new requests will not be
     * added to the queue and all processing will halt.
     */
    @Override
    public void close() {
        log.info("[Tokenizer] Shutdown Requested");
        running.set(false);
    }

    /**
     * Return the number of active requests out there
     *
     * @return the number of active requests
     */
    public int activeCount() {
        int size = 0;
        if (requests != null) {
            size = requests.size();
        }
        if (buffer != null) {
            size += buffer.size();
        }
        return size;
    }

    @Override
    public void run() {
        log.info("[Tokenizer] Starting");
        while (running.get()) {
            try {
                int drained = Queues.drain(requests, buffer, maxQueueLength, maxWaitTime, TimeUnit.MILLISECONDS);
                if (drained > 0 && running.get()) {
                    // a few questions we should answer about this:
                    //   - should it block? (the ITRB does by acquiring a lock)
                    //   - without blocking the queue can fill up and we can have requests denied
                    //   - if not, should we pass it to an Executor?
                    processRequests(buffer);
                    buffer.clear();
                }
            } catch (InterruptedException ex) {
                log.error("[Tokenizer] Error draining BlockingQueue during tokenization", ex);
            } catch (Exception e) {
                log.error("[Tokenizer] Uncaught exception!", e);
            }
        }

        // why not
        buffer.clear();
        entries.clear();
        requests.clear();
        log.info("[Tokenizer] Finished");
    }

    /**
     * Send TokenRequests to the IMS in order to get ACE Tokens. Then spawn new threads to register
     * the ACE Tokens with the Chronopolis Ingest Server
     *
     * There are two types of failure which can happen:
     * - ImsException: Remove all requests from the entries map; allow to be requeue'd at a later time
     * - Ingest Exception: Retry 3 times (handled by the Registrar), then abort
     *
     * @param requests the TokenRequests to send
     */
    private void processRequests(List<TokenRequest> requests) {
        log.info("[Tokenizer] Sending batch: {} requests", requests.size());

        final String imsHost = ims.configuration().getEndpoint();
        try {
            List<TokenResponse> responses = ims.requestTokensImmediate(tokenClass, requests);
            responses.forEach(response -> {
                ManifestEntry entry = entries.get(response.getName());
                if (entry == null) {
                    log.error("[Tokenizer] Unable to find correlated Bag for Token {}", response.getName());
                } else {
                    // this now makes multiple attempts to register a token before failing
                    // the big question is whether or not we want to maintain a journal of tokens which we
                    // need to register. imo this would be a good thing to build in as a second version
                    // of the implementation as we would need to bring in some more dependencies, update
                    // the filtering, etc.
                    TokenRegistrar registrar = new TokenRegistrar(tokens, entry, response, imsHost);
                    TokenResponse tr = registrar.get();
                    if (tr != null) {
                        removeEntry(tr.getName());
                    }
                }
            });
        } catch (Exception e) {
            log.error("[Tokenizer] Exception on send {}", e.getMessage(), e);
            requests.forEach(request -> removeEntry(request.getName()));
        } finally {
            requests.clear();
        }
    }

    /**
     * Remove an entry from the processing map
     *
     * @param response  the TokenResponse to remove
     * @param throwable a thrown exception if it occurred
     */
    private void removeEntry(TokenResponse response, Throwable throwable) {
        if (removeEntry(response.getName()) == null) {
            log.warn("[Tokenizer] Unable to remove {} from processing", response.getName());
        }

        // requeue here?
        if (throwable != null) {
            log.warn("[Tokenizer] Received throwable downchain", throwable);
        }
    }

    /**
     * Remove and entry from the processing map
     *
     * @param name the name of the TokenRequest to remove
     * @return the associated ManifestEntry or null if none exist
     */
    private ManifestEntry removeEntry(String name) {
        return entries.remove(name);
    }

    /**
     * Create a TokenRequest from a ManifestEntry
     *
     * @param entry the ManifestEntry to create on
     * @return the TokenRequest
     */
    private TokenRequest createRequest(ManifestEntry entry) {
        TokenRequest request = new TokenRequest();
        request.setName(entry.tokenName());
        request.setHashValue(entry.getCalculatedDigest());
        return request;
    }

    /**
     * Delegate class which we can mock for testing. Since we only need a small subset of the
     * IMSService methods, we only implement what we need.
     */
    public static class ImsServiceWrapper {

        private AceConfiguration.Ims configuration;

        public ImsServiceWrapper(AceConfiguration.Ims configuration) {
            this.configuration = configuration;
        }

        public AceConfiguration.Ims configuration() {
            return configuration;
        }

        public IMSService connect() {
            return IMSService.connect(configuration.getEndpoint(), configuration.getPort(), configuration.isSsl());
        }

        public List<TokenResponse> requestTokensImmediate(String tokenClass, List<TokenRequest> requests) {
            return connect().requestTokensImmediate(tokenClass, requests);
        }

    }

    @VisibleForTesting
    protected ConcurrentHashMap<String, ManifestEntry> getEntries() {
        return entries;
    }

}
