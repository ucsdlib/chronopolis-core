package org.chronopolis.tokenize.supervisor;

import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.rest.kot.models.Bag;
import org.chronopolis.rest.kot.models.enums.BagStatus;
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
    private final Bag bag = new Bag(1L, 1L, 1L, null, null, now(), now(),
            BAG_NAME, CREATOR_NAME, DEPOSITOR_NAME, BagStatus.DEPOSITED, new HashSet<>());

    // Start

    @Test
    public void startExceedsLimit() throws InterruptedException {
        // todo: unhardcode this
        for (int i = 0; i < 5000; i++) {
            boolean started = supervisor.start(
                    new ManifestEntry(bag, "start-success-" + i, "registered-digest"));
            Assert.assertTrue(started);
        }

        boolean interrupted = false;
        try {
            CompletableFuture.supplyAsync(() -> supervisor.start(new ManifestEntry(bag,
                    "start-failure", "registered-digest")))
                    .get(100, TimeUnit.MILLISECONDS);
        } catch (ExecutionException | TimeoutException e) {
            interrupted = true;
        }

        Assert.assertTrue(interrupted);
    }

    @Test
    public void startRejectsTrackedEntry() {
        ManifestEntry exists = new ManifestEntry(bag, "exists", "registered-digest");
        Map<ManifestEntry, WorkUnit> processing = supervisor.getProcessing();
        processing.put(exists, new WorkUnit());

        Assert.assertFalse(supervisor.start(exists));
        Assert.assertEquals(5000, supervisor.availablePermits());
    }

    // Tokenize

    @Test
    public void retryTokenize() {
        ManifestEntry retry = new ManifestEntry(bag, "retry-register", "registered-digest");
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
        ManifestEntry noEncapsulator = new ManifestEntry(bag, "no-enc", "registered-digest");
        ManifestEntry invalidState = new ManifestEntry(bag, "invalid-state", "registered-digest");

        Map<ManifestEntry, WorkUnit> processing = supervisor.getProcessing();
        processing.put(invalidState, new WorkUnit());
        Assert.assertFalse(supervisor.retryTokenize(invalidState));
        Assert.assertFalse(supervisor.retryTokenize(noEncapsulator));
    }

    // Associate

    @Test
    public void associate() {
        ManifestEntry assoc = new ManifestEntry(bag, "associate", "registered-digest");
        supervisor.start(assoc);

        Set<ManifestEntry> entries = supervisor.queuedEntries(1, 10, TimeUnit.MILLISECONDS);
        entries.forEach(entry -> supervisor.associate(entry, new TokenResponse()));

        Assert.assertTrue(supervisor.isProcessing(assoc));
    }

    @Test
    public void associateRejectsNotRequesting() {
        ManifestEntry queued = new ManifestEntry(bag, "queued", "registered-digest");
        ManifestEntry noResponse = new ManifestEntry(bag, "no-response", "registered-digest");
        Map<ManifestEntry, WorkUnit> processing = supervisor.getProcessing();
        processing.put(queued, new WorkUnit());

        Assert.assertFalse(supervisor.associate(queued, new TokenResponse()));
        Assert.assertFalse(supervisor.associate(noResponse, new TokenResponse()));
    }

    @Test
    public void associateRejectsTokenConflict() {
        ManifestEntry conflict = new ManifestEntry(bag, "conflict", "registered-digest");
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
        ManifestEntry assoc = new ManifestEntry(bag, "associate", "registered-digest");
        supervisor.start(assoc);

        Set<ManifestEntry> entries = supervisor.queuedEntries(1, 10, TimeUnit.MILLISECONDS);
        entries.forEach(entry -> supervisor.associate(entry, new TokenResponse()));

        Map<ManifestEntry, TokenResponse> tokens =
                supervisor.tokenizedEntries(1, 10, TimeUnit.MILLISECONDS);
        tokens.forEach((e, t) -> Assert.assertTrue(supervisor.retryRegister(e)));
    }

    @Test
    public void retryRegisterRejectsInvalidEntry() {
        ManifestEntry noResponse = new ManifestEntry(bag, "no-response", "registered-digest");
        ManifestEntry invalidState = new ManifestEntry(bag, "invalid-state", "registered-digest");

        Map<ManifestEntry, WorkUnit> processing = supervisor.getProcessing();
        processing.put(invalidState, new WorkUnit());

        Assert.assertFalse(supervisor.retryRegister(noResponse));
        Assert.assertFalse(supervisor.retryRegister(invalidState));
    }

    @Test
    public void complete() {
        ManifestEntry complete = new ManifestEntry(bag, "complete", "registered-digest");
        supervisor.start(complete);
        supervisor.complete(complete);
        Assert.assertFalse(supervisor.isProcessing(complete));
    }

    /**
     * Simple test to validate that Entries which have not been submitted are rejected
     */
    @Test
    public void notStartedEntryOperations() {
        ManifestEntry untracked = new ManifestEntry(bag, "untracked", "registered-digest");

        Assert.assertFalse(supervisor.retryRegister(untracked));
        Assert.assertFalse(supervisor.retryTokenize(untracked));
        Assert.assertFalse(supervisor.isProcessing(untracked));
    }

}