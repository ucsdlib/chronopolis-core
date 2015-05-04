package org.chronopolis.replicate.batch;

import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Step for handling interaction with ACE. We do a few things:
 * * Register a collection
 * * Upload the token store
 * * Start an initial audit
 *
 *
 * Created by shake on 8/22/14.
 */
public class AceRegisterStep implements Tasklet {
    private final Logger log = LoggerFactory.getLogger(AceRegisterStep.class);

    private AceService aceService;
    private ReplicationSettings settings;
    private ReplicationNotifier notifier;
    private String collection;
    private String depositor;
    private String fixityAlgorithm;
    private String tokenLocation;
    private int auditPeriod;

    public AceRegisterStep(AceService aceService,
                           ReplicationSettings settings,
                           CollectionInitMessage message,
                           ReplicationNotifier notifier) {
        this.aceService = aceService;
        this.settings = settings;
        this.notifier = notifier;
        this.collection = message.getCollection();
        this.depositor = message.getDepositor();
        this.fixityAlgorithm = message.getFixityAlgorithm();
        this.auditPeriod = message.getAuditPeriod();
    }

    public AceRegisterStep(AceService aceService,
                           ReplicationSettings settings,
                           ReplicationNotifier notifier,
                           Replication replication) {
        this.aceService = aceService;
        this.settings = settings;
        this.notifier = notifier;

        Bag bag = replication.getBag();
        this.collection = bag.getName();
        this.depositor = bag.getDepositor();
        this.fixityAlgorithm = bag.getFixityAlgorithm();
        this.tokenLocation = bag.getTokenLocation();
        this.auditPeriod = 90;
    }

    /**
     * Add a collection and token store to ACE
     *
     *
     * @param stepContribution
     * @param chunkContext
     * @return
     * @throws Exception
     */
    @Override
    public RepeatStatus execute(final StepContribution stepContribution, final ChunkContext chunkContext) throws Exception {
        // Setup our collection settings
        // int auditPeriod = message.getAuditPeriod();
        // String collection = message.getCollection();
        // String depositor = message.getDepositor();
        // String fixityAlgorithm = message.getFixityAlgorithm();

        // And an atomic for synchronous calls for retrofit
        // TODO: Why not use a lock?
        final AtomicBoolean callbackComplete = new AtomicBoolean(false);


        Path collectionPath = Paths.get(settings.getPreservation(), depositor, collection);
        // TODO: Get stage for manifest?
        Path manifest = Paths.get(settings.getPreservation(), tokenLocation);

        log.trace("Building ACE json");
        GsonCollection aceGson = new GsonCollection.Builder()
                .name(collection)
                .digestAlgorithm(fixityAlgorithm)
                .directory(collectionPath.toString())
                .group(depositor)
                .storage("local")
                .auditPeriod(String.valueOf(auditPeriod))
                .auditTokens("true")
                .proxyData("false")
                .build();

        log.debug("POSTing {}", aceGson.toJsonJackson());
        Map<String, Integer> idMap = null;
        try {
            idMap = aceService.addCollection(aceGson);
            log.info("Successfully registered collection {}", collection);
        } catch (RetrofitError error) {
            log.error("Error registering ACE collection. Response code {} with reason {}",
                    error.getResponse().getStatus(), error.getResponse().getReason());
            notifier.setSuccess(false);
            notifier.setAceStep(error.getResponse().getReason());
            throw new RuntimeException(error);
        }

        long id = idMap.get("id");
        final String[] statusMessage = {"success"};

        Callback tsCallback = new Callback() {
            @Override
            public void success(final Object o, final Response response) {
                log.info("Successfully posted token store");
                callbackComplete.getAndSet(true);
            }

            @Override
            public void failure(final RetrofitError retrofitError) {
                log.error("Error posting token store {} {}",
                        retrofitError.getResponse().getStatus(),
                        retrofitError.getBody());
                notifier.setSuccess(false);
                statusMessage[0] = retrofitError.getResponse().getReason();
                callbackComplete.getAndSet(true);
            }
        };

        log.info("Loading token store for {}...", collection);
        aceService.loadTokenStore(id, new TypedFile("ASCII Text", manifest.toFile()), tsCallback);

        // Since the callback is asynchronous, we need to wait for it to complete before moving on
        log.trace("Waiting for token register to complete");
        waitForCallback(callbackComplete);
        callbackComplete.set(false);

        aceService.startAudit(id, new Callback<Void>() {
            @Override
            public void success(final Void aVoid, final Response response) {
                log.info("Successfully started audit");
                callbackComplete.set(true);
            }

            @Override
            public void failure(final RetrofitError error) {
                log.info("Could not start audit. {} {}",
                        error.getResponse().getStatus(),
                        error.getResponse().getReason());
                notifier.setSuccess(false);
                statusMessage[0] = error.getResponse().getReason();
                callbackComplete.set(true);
            }
        });
        log.trace("Waiting for audit start to complete");
        waitForCallback(callbackComplete);

        notifier.setAceStep(statusMessage[0]);
        return RepeatStatus.FINISHED;
    }

    private void waitForCallback(AtomicBoolean callbackComplete) {
        while (!callbackComplete.get()) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error("Sleep interrupted", e);
            }
        }
     }

}
