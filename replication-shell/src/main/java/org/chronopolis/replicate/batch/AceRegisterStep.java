package org.chronopolis.replicate.batch;

import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.replicate.config.ReplicationSettings;
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
 * Created by shake on 8/22/14.
 */
public class AceRegisterStep implements Tasklet {
    private final Logger log = LoggerFactory.getLogger(AceRegisterStep.class);

    private AceService aceService;
    private ReplicationSettings settings;
    private CollectionInitMessage message;

    public AceRegisterStep(AceService aceService, ReplicationSettings settings, CollectionInitMessage message) {
        this.aceService = aceService;
        this.settings = settings;
        this.message = message;
    }

    @Override
    public RepeatStatus execute(final StepContribution stepContribution, final ChunkContext chunkContext) throws Exception {
        // Setup our collection settings
        int auditPeriod = message.getAuditPeriod();
        String collection = message.getCollection();
        String depositor = message.getDepositor();
        String fixityAlgorithm = message.getFixityAlgorithm();

        // And an atomic for synchronous calls for retrofit
        // TODO: Why not use a lock?
        final AtomicBoolean callbackComplete = new AtomicBoolean(false);


        Path collectionPath = Paths.get(settings.getPreservation(), depositor, collection);
        // TODO: Get stage for manifest?
        Path manifest = Paths.get(settings.getPreservation(), collection+"-tokens");

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
            throw new RuntimeException(error);
        }

        long id = idMap.get("id");

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
                callbackComplete.getAndSet(true);
            }
        };

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
                callbackComplete.set(true);
            }
        });
        log.trace("Waiting for audit start to complete");
        waitForCallback(callbackComplete);


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
