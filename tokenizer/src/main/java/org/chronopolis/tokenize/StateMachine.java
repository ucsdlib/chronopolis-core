package org.chronopolis.tokenize;

import edu.umiacs.ace.ims.ws.TokenResponse;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Some description here of what this does
 *
 * @author shake
 */
public interface StateMachine {

    void start(ManifestEntry entry);

    void retryTokenize(ManifestEntry entry);
    void retryRegister(ManifestEntry entry);

    void associate(ManifestEntry entry, TokenResponse response);

    void complete(ManifestEntry entry);

    // Polling operations

    /**
     * Operation which polls the StateMachine for ManifestEntries which are have been requested for
     * processing but have not yet been started on. Waits for a given timeout to retrieve up to n
     * Entries.
     *
     * If an InterruptedException is thrown, it is caught and the Threads state is set to
     * interrupted
     *
     * @param size the maximum amount of entries to get
     * @param timeout the length of the timeout to wait for
     * @param timeUnit the unit of time to wait
     * @return any ManifestEntries which have been requested for processing
     */
    Set<ManifestEntry> queuedEntries(int size, long timeout, TimeUnit timeUnit);

    /**
     * Polls the StateMachine for ManifestEntries which have associated TokenResponses and are ready
     * to be ingested into the Chronopolis Ingest Server.
     *
     * @param size the maximum amount of entries to get
     * @param timeout the length of the timeout to wait for
     * @param timeUnit the unit of time to wait
     * @return any ManifestEntries which have an associated TokenResponse
     */
    Map<ManifestEntry, TokenResponse> tokenizedEntries(int size, long timeout, TimeUnit timeUnit);

}
