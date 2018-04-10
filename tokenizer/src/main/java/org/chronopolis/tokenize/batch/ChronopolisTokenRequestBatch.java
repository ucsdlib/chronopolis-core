package org.chronopolis.tokenize.batch;

import edu.umiacs.ace.ims.ws.TokenRequest;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.TokenWorkSupervisor;
import org.chronopolis.tokenize.config.TokenTaskConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Entry point for TokenRequest creation. This keeps a mapping of which token request
 * belongs to which Bag so that it can be later updated to the Ingest Server for Chronopolis.
 * <p>
 * Based off of {@link edu.umiacs.ace.ims.api.ImmediateTokenRequestBatch}. Different in that
 * instead of running the request threads itself, this creates {@link Runnable} objects which
 * are a single set of requests.
 * <p>
 * todo: should we have an interface for closing/stopping the RequestBatch?
 *
 * @author shake
 */
public class ChronopolisTokenRequestBatch implements TokenRequestBatch, Runnable {

    private final Logger log = LoggerFactory.getLogger(TokenTaskConfiguration.TOKENIZER_LOG_NAME);

    private final String tokenClass;
    private final int maxWaitTime;
    private final int maxQueueLength;
    private final ImsServiceWrapper ims;
    private final TokenWorkSupervisor supervisor;

    private AtomicBoolean running = new AtomicBoolean(true);

    public ChronopolisTokenRequestBatch(AceConfiguration configuration,
                                        TokenWorkSupervisor supervisor) {
        this(configuration, new ImsServiceWrapper(configuration.getIms()), supervisor);
    }

    public ChronopolisTokenRequestBatch(AceConfiguration configuration,
                                        ImsServiceWrapper wrapper,
                                        TokenWorkSupervisor supervisor) {
        this.ims = wrapper;
        this.supervisor = supervisor;

        AceConfiguration.Ims imsConfiguration = configuration.getIms();
        tokenClass = imsConfiguration.getTokenClass();
        maxWaitTime = imsConfiguration.getWaitTime();
        maxQueueLength = imsConfiguration.getQueueLength();
    }

    @Override
    public void run() {
        log.info("[Tokenizer] Starting");
        while (running.get()) {
            Set<ManifestEntry> entries = supervisor.queuedEntries(maxQueueLength,
                    maxWaitTime,
                    TimeUnit.MILLISECONDS);
            process(entries);
        }

        log.info("[Tokenizer] Finished");
    }

    /**
     * Send TokenRequests to the IMS in order to get ACE Tokens. Then spawn new threads to register
     * the ACE Tokens with the Chronopolis Ingest Server
     * <p>
     * If an error occurs communicating with the IMS, each {@link ManifestEntry} is resubmitted
     * through the {@link TokenWorkSupervisor} retry method.
     *
     * @param entrySet the {@link ManifestEntry}s to process and create Ace Tokens for
     */
    @Override
    public void process(Set<ManifestEntry> entrySet) {
        if (entrySet.isEmpty() || !running.get()) {
            return;
        }

        // we could also use an encapsulating class but let's not worry too much at the moment
        // track our TokenRequests and the ManifestEntry associated with each request
        Map<String, ManifestEntry> entries = new HashMap<>();
        List<TokenRequest> requests = new ArrayList<>();
        entrySet.forEach(entry -> {
            requests.add(createRequest(entry));
            entries.put(entry.tokenName(), entry);
        });

        log.info("[Tokenizer] Sending batch: {} requests", requests.size());
        try {
            // todo: is it possible for a list to be returned with an incorrect number of responses?
            // if so it might be good to protect against it proactively but at the moment lets get
            // the base impl down
            List<TokenResponse> responses = ims.requestTokensImmediate(tokenClass, requests);
            log.debug("Got back {} responses", responses.size());
            responses.forEach(response -> {
                log.trace("[{}] Processing response", response.getName());
                ManifestEntry manifestEntry = entries.get(response.getName());
                supervisor.associate(manifestEntry, response);
            });
        } catch (Exception e) {
            log.error("[Tokenizer] Exception on send {}", e.getMessage(), e);
            entries.values().forEach(supervisor::retryTokenize);
        } finally {
            // Not really necessary...
            requests.clear();
            entries.clear();
            entrySet.clear();
        }
    }

    public void close() {
        running.set(false);
    }

}
