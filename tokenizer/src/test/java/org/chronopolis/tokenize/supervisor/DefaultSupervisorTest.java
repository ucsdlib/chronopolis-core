package org.chronopolis.tokenize.supervisor;

import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.tokenize.ManifestEntry;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class DefaultSupervisorTest {

    private final Logger log = LoggerFactory.getLogger(DefaultSupervisorTest.class);
    private final DefaultSupervisor supervisor = new DefaultSupervisor();

    private final Bag bag = new Bag()
            .setName("test-bag")
            .setDepositor("test-depositor");

    // Start

    @Test
    public void startExceedsLimit() throws InterruptedException {
        // todo: unhardcode this
        for (int i = 0; i < 5000; i++) {
            boolean started = supervisor.start(
                    new ManifestEntry(bag, "start-success-" + i, "registered-digest"));
            Assert.assertTrue(started);
        }

        // Not really sure about the best way to test this, but basically we'll block forever and we
        // don't want that so interrupt the current thread
        // Might be best just to add limits into the Supervisor so we don't block forever idk
        Thread thread = Executors.defaultThreadFactory()
                .newThread(new Interrupter(Thread.currentThread()));
        thread.start();
        log.info("adding entry with no slots left");
        boolean start = supervisor.start(new ManifestEntry(bag, "start-fail", "registered-digest"));
        thread.join();
        Assert.assertFalse(start);
        Assert.assertTrue(Thread.currentThread().isInterrupted());
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
        ManifestEntry invalidState=  new ManifestEntry(bag, "invalid-state", "registered-digest");

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

    public class Interrupter implements Runnable {

        private final Thread toInterrupt;

        public Interrupter(Thread toInterrupt) {
            this.toInterrupt = toInterrupt;
        }

        @Override
        public void run() {
            try {
                log.info("sleeping before interrupt");
                TimeUnit.MILLISECONDS.sleep(100);
                log.info("interrupt");
                toInterrupt.interrupt();
            } catch (InterruptedException e) {
                log.error("We were interrupted!");
            }
        }
    }
}