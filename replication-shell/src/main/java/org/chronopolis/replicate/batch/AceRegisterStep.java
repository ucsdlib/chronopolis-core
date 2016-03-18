package org.chronopolis.replicate.batch;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
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
@Deprecated
public class AceRegisterStep implements Tasklet {
    private final Logger log = LoggerFactory.getLogger(AceRegisterStep.class);

    private IngestAPI ingest;
    private AceService aceService;
    private ReplicationSettings settings;
    private ReplicationNotifier notifier;
    private String collection;
    private String depositor;
    private String fixityAlgorithm;
    private String tokenLocation;
    private int auditPeriod;

    public AceRegisterStep(IngestAPI ingest,
                           AceService aceService,
                           ReplicationSettings settings,
                           ReplicationNotifier notifier,
                           Replication replication) {
        this.ingest = ingest;
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
     * Ok so now we have 3 steps in ACE and 3 status updates for the ingest server
     * - ACE_REGISTERED
     * - ACE_TOKEN_LOADED
     * - ACE_AUDITING
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
        Map<String, Long> idMap = loadCollection(aceGson);

        long id = idMap.get("id");
        final String[] statusMessage = {"success"};
        Callback<Void> tsCallback = new Callback<Void>() {
            @Override
            public void onResponse(Response response) {
                log.info("Successfully posted token store");
                callbackComplete.getAndSet(true);
            }

            @Override
            public void onFailure(Throwable throwable) {
                                log.error("Error POSTing token store {} {}",
                                        throwable.getMessage(), throwable);
                notifier.setSuccess(false);
                statusMessage[0] = "Error loading token store: "
                        + throwable.getMessage();
                callbackComplete.getAndSet(true);
            }
        };

        log.info("Loading token store for {}...", collection);
        Call<Void> call = aceService.loadTokenStore(id, RequestBody.create(MediaType.parse("ASCII Text"), manifest.toFile()));
        call.enqueue(tsCallback);

        // Since the callback is asynchronous, we need to wait for it to complete before moving on
        // TODO: Should use something like the SimpleCallback to wait for it to complete
        //       or we could wrap it in a try/catch
        log.trace("Waiting for token register to complete");
        waitForCallback(callbackComplete);
        callbackComplete.set(false);

        Call<Void> auditCall = aceService.startAudit(id);

        try {
            Response<Void> execute = auditCall.execute();
            if (!execute.isSuccess()) {
                throw new IOException(execute.message());
            }
        } catch (IOException e) {
            log.error("Error starting audit", e);
            notifier.setSuccess(false);
            statusMessage[0] = "Error starting audit";
        }
        log.trace("Waiting for audit start to complete");
        // waitForCallback(callbackComplete);

        notifier.setAceStep(statusMessage[0]);
        return RepeatStatus.FINISHED;
    }

    /**
     * Register a collection into ACE
     *
     * @param collection The collection to register
     * @return Response from ACE containing the id of the collection
     */
    private Map<String, Long> loadCollection(GsonCollection collection) {
        try {
            Call<Map<String, Long>> addCall = aceService.addCollection(collection);
            Response<Map<String, Long>> response = addCall.execute();
            if (!response.isSuccess()) {
                log.error("Error registering ACE collection. Response code {} with reason {}",
                        response.code(),
                        response.message());
                throw new RuntimeException(response.raw().request().url() + ": " + response.message());
            }

            Map<String, Long> idMap = response.body();
            log.info("Successfully registered collection {}", collection);
            return idMap;
        } catch (IOException e) {
            log.error("Error communicating with ACE server {}",
                    e.getMessage(), e);
            notifier.setSuccess(false);
            notifier.setAceStep("Error registering collection: "
                    + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private void waitForCallback(AtomicBoolean complete) {
        while (!complete.get()) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.error("Sleep interrupted", e);
            }
        }
    }

}
