package org.chronopolis.tokenize.supervisor;

import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.tokenize.ManifestEntry;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.time.ZonedDateTime.now;

public class DefaultSupervisorTest {

    private final DefaultSupervisor supervisor = new DefaultSupervisor();

    private final String BAG_NAME = "test-bag";
    private final String CREATOR_NAME = "default-supervisor-test";
    private final String DEPOSITOR_NAME = "test-depositor";
    private final String START_FAILURE = "start-failure";
    private final String REGISTERED_DIGEST = "registered-digest";
    private final Bag bag = new Bag(1L, 1L, 1L, null, null, now(), now(),
            BAG_NAME, CREATOR_NAME, DEPOSITOR_NAME, BagStatus.DEPOSITED, new HashSet<>());

    // Start

    @Test
    public void startExceedsLimit() throws InterruptedException {
        String START_SUCCESS_PREFIX = "start-success-";
        for (int i = 0; i < 5000; i++) {
            boolean started = supervisor.start(
                    new ManifestEntry(bag, START_SUCCESS_PREFIX + i, REGISTERED_DIGEST));
            Assert.assertTrue(started);
        }

        boolean interrupted = false;
        try {
            CompletableFuture.supplyAsync(() -> supervisor.start(new ManifestEntry(bag,
                    START_FAILURE, REGISTERED_DIGEST)))
                    .get(100, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            interrupted = true;
        }

        Assert.assertTrue(interrupted);
    }

    @Test
    public void startRejectsTrackedEntry() {
        ManifestEntry exists = new ManifestEntry(bag, "exists", REGISTERED_DIGEST);
        Map<ManifestEntry, WorkUnit> processing = supervisor.getProcessing();
        processing.put(exists, new WorkUnit());

        Assert.assertFalse(supervisor.start(exists));
        Assert.assertEquals(5000, supervisor.availablePermits());
    }

    // Tokenize

    @Test
    public void retryTokenize() {
        ManifestEntry retry = new ManifestEntry(bag, "retry-register", REGISTERED_DIGEST);
        supervisor.start(retry);
        Set<ManifestEntry> entries = supervisor.queuedEntries(1, 10, TimeUnit.MILLISECONDS);
        Assert.assertFalse(entries.isEmpty());

        entries.forEach(entry -> {
            boolean restarted = supervisor.retryTokenize(entry);
            Assert.assertTrue(restarted);
        });

        Assert.assertTrue(supervisor.isProcessing(retry));
    }

    @Test
    public void retryTokenizeRejectsInvalidEntry() {
        ManifestEntry noEncapsulator = new ManifestEntry(bag, "no-enc", REGISTERED_DIGEST);
        ManifestEntry invalidState = new ManifestEntry(bag, "invalid-state", REGISTERED_DIGEST);

        Map<ManifestEntry, WorkUnit> processing = supervisor.getProcessing();
        processing.put(invalidState, new WorkUnit());
        Assert.assertFalse(supervisor.retryTokenize(invalidState));
        Assert.assertFalse(supervisor.retryTokenize(noEncapsulator));
    }

    // Associate

    @Test
    public void associate() {
        ManifestEntry assoc = new ManifestEntry(bag, "associate", REGISTERED_DIGEST);
        supervisor.start(assoc);

        Set<ManifestEntry> entries = supervisor.queuedEntries(1, 10, TimeUnit.MILLISECONDS);
        entries.forEach(entry -> supervisor.associate(entry, new TokenResponse()));

        Assert.assertTrue(supervisor.isProcessing(assoc));
    }

    @Test
    public void associateRejectsNotRequesting() {
        ManifestEntry queued = new ManifestEntry(bag, "queued", REGISTERED_DIGEST);
        ManifestEntry noResponse = new ManifestEntry(bag, "no-response", REGISTERED_DIGEST);
        Map<ManifestEntry, WorkUnit> processing = supervisor.getProcessing();
        processing.put(queued, new WorkUnit());

        Assert.assertFalse(supervisor.associate(queued, new TokenResponse()));
        Assert.assertFalse(supervisor.associate(noResponse, new TokenResponse()));
    }

    @Test
    public void associateRejectsTokenConflict() {
        ManifestEntry conflict = new ManifestEntry(bag, "conflict", REGISTERED_DIGEST);
        WorkUnit enc = new WorkUnit();
        enc.setState(WorkUnit.State.QUEUED_FOR_REGISTRATION);
        enc.setResponse(new TokenResponse());

        Map<ManifestEntry, WorkUnit> processing = supervisor.getProcessing();
        processing.put(conflict, enc);

        Assert.assertFalse(supervisor.associate(conflict, new TokenResponse()));
    }

    // todo
    // public void associateInterrtuped() {
    // }

    @Test
    public void retryRegister() {
        ManifestEntry assoc = new ManifestEntry(bag, "associate", REGISTERED_DIGEST);
        supervisor.start(assoc);

        Set<ManifestEntry> entries = supervisor.queuedEntries(1, 10, TimeUnit.MILLISECONDS);
        entries.forEach(entry -> supervisor.associate(entry, new TokenResponse()));

        Map<ManifestEntry, TokenResponse> tokens =
                supervisor.tokenizedEntries(1, 10, TimeUnit.MILLISECONDS);
        tokens.forEach((e, t) -> Assert.assertTrue(supervisor.retryRegister(e)));
    }

    @Test
    public void retryRegisterRejectsInvalidEntry() {
        ManifestEntry noResponse = new ManifestEntry(bag, "no-response", REGISTERED_DIGEST);
        ManifestEntry invalidState = new ManifestEntry(bag, "invalid-state", REGISTERED_DIGEST);

        Map<ManifestEntry, WorkUnit> processing = supervisor.getProcessing();
        processing.put(invalidState, new WorkUnit());

        Assert.assertFalse(supervisor.retryRegister(noResponse));
        Assert.assertFalse(supervisor.retryRegister(invalidState));
    }

    @Test
    public void complete() {
        ManifestEntry complete = new ManifestEntry(bag, "complete", REGISTERED_DIGEST);
        supervisor.start(complete);
        supervisor.complete(complete);
        Assert.assertFalse(supervisor.isProcessing(complete));
    }

    /**
     * Simple test to validate that Entries which have not been submitted are rejected
     */
    @Test
    public void notStartedEntryOperations() {
        ManifestEntry untracked = new ManifestEntry(bag, "untracked", REGISTERED_DIGEST);

        Assert.assertFalse(supervisor.retryRegister(untracked));
        Assert.assertFalse(supervisor.retryTokenize(untracked));
        Assert.assertFalse(supervisor.isProcessing(untracked));
    }

}