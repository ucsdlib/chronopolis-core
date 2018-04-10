package org.chronopolis.tokenize;

import edu.umiacs.ace.ims.ws.TokenResponse;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * An object that tracks {@link ManifestEntry}s which need to have ACE Tokens created and are
 * actively being processed and worked on. Methods are provided to control the state of a
 * {@link ManifestEntry} as it goes through its lifecycle, and implementations should make sure that
 * the updates to an Entry are valid. Work can be polled from a supervisor using the queuedEntries
 * and tokenizedEntries methods.
 *
 * Note that at the moment states are implicit. Still getting feedback for should be returned from
 * each state transition.
 *
 * The lifecycle is as follows:
 * start -> {queuedEntries}
 * queuedEntries -> {retryTokenize, associate (puts in tokenizedEntries)}
 * tokenizedEntries -> {retryRegister, complete}
 * complete -> {}
 *
 * @author shake
 */
public interface TokenWorkSupervisor {

    /**
     * Add a {@link ManifestEntry} to a Supervisor for processing and creation of its ACE Token
     *
     * @param entry the ManifestEntry to add
     */
    void start(ManifestEntry entry);

    /**
     * Re-submit a {@link ManifestEntry} to a Supervisor to attempt to create an ACE Token again
     *
     * @param entry the ManifestEntry being resubmitted
     */
    void retryTokenize(ManifestEntry entry);

    /**
     * Re-submit a {@link ManifestEntry} to a Supervisor to attempt to register its ACE Token to the
     * Chronopolis Ingest Server again
     *
     * @param entry the ManifestEntry being resubmitted
     */
    void retryRegister(ManifestEntry entry);

    /**
     * Attach an ACE {@link TokenResponse} to a {@link ManifestEntry}
     *
     * @param entry    the ManifestEntry which was successfully tokenized
     * @param response the TokenResponse being associated
     */
    void associate(ManifestEntry entry, TokenResponse response);

    /**
     * Mark a {@link ManifestEntry} as having completed its lifecycle. At the moment this does not
     * mean that it necessarily completed successfully, only that it has no more actionable states.
     *
     * @param entry the ManifestEntry which has completed its lifecycle
     */
    void complete(ManifestEntry entry);

    // Polling operations

    /**
     * Operation which polls the Supervisor for ManifestEntries which are have been requested for
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
     * Polls the Supervisor for ManifestEntries which have associated TokenResponses and are ready
     * to be ingested into the Chronopolis Ingest Server.
     *
     * @param size the maximum amount of entries to get
     * @param timeout the length of the timeout to wait for
     * @param timeUnit the unit of time to wait
     * @return any ManifestEntries which have an associated TokenResponse
     */
    Map<ManifestEntry, TokenResponse> tokenizedEntries(int size, long timeout, TimeUnit timeUnit);

}
