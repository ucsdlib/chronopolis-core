package org.chronopolis.replicate.batch.ace;

import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.batch.callback.UpdateCallback;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Phaser;

/**
 *
 * Created by shake on 3/8/16.
 */
public class AceRegisterTasklet implements Callable<Long> {
    private final Logger log = LoggerFactory.getLogger(AceRegisterTasklet.class);

    private IngestAPI ingest;
    private AceService aceService;
    private Replication replication;
    private ReplicationSettings settings;
    private ReplicationNotifier notifier;

    private Long id = -1L;

    private final Phaser phaser;

    public AceRegisterTasklet(IngestAPI ingest, AceService aceService, Replication replication, ReplicationSettings settings, ReplicationNotifier notifier) {
        this.ingest = ingest;
        this.aceService = aceService;
        this.replication = replication;
        this.settings = settings;
        this.notifier = notifier;

        // Phaser for main thread + callback
        phaser = new Phaser();
    }

    public void run() throws Exception {
        log.trace("Building ACE json");
        Bag bag = replication.getBag();

        // What we want to do:
        // get bag.name/bag.collection
        // -> 200 -> return id
        // -> 204 -> register

        phaser.register();
        getId(bag);
        phaser.arriveAndAwaitAdvance();

        if (id == -1) {
            // register and what not
            register(bag);

            // main thread await
            phaser.arriveAndAwaitAdvance();
        }
    }

    private void register(Bag bag) {
        Path collectionPath = Paths.get(settings.getPreservation(),
                bag.getDepositor(),
                bag.getName());

        GsonCollection aceGson = new GsonCollection.Builder()
                .name(bag.getName())
                .digestAlgorithm(bag.getFixityAlgorithm())
                .directory(collectionPath.toString())
                .group(bag.getDepositor())
                .storage("local")
                .auditPeriod(String.valueOf(settings.getAuditPeriod()))
                .auditTokens("true")
                .proxyData("false")
                .build();

        log.debug("POSTing {}", aceGson.toJsonJackson());

        // callback register
        phaser.register();

        Call<Map<String, Long>> call = aceService.addCollection(aceGson);
        call.enqueue(new Callback<Map<String, Long>>() {
            @Override
            public void onResponse(Response<Map<String, Long>> response) {
                if (response.isSuccess()) {
                    id = response.body().get("id");
                    setRegistered();
                } else {
                    log.error("Error registering collection in ACE: {} - {}", response.code(), response.message());
                    try {
                        log.debug("{}", response.errorBody().string());
                    } catch (IOException ignored) {
                    }

                    notifier.setSuccess(false);
                }

                // arrive and dereg?
                phaser.arrive();
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("Error communicating with ACE", throwable);
                notifier.setSuccess(false);
                phaser.arrive();
                // ..?
                // throw new RuntimeException(throwable);
            }
        });
    }

    private void getId(Bag bag) {
        // Register with the phaser so that we may arrive when the callback completes
        phaser.register();

        Call<GsonCollection> call = aceService.getCollectionByName(bag.getName(), bag.getDepositor());
        call.enqueue(new Callback<GsonCollection>() {
            @Override
            public void onResponse(Response<GsonCollection> response) {
                if (response.isSuccess() && response.body() != null) {
                    id = response.body().getId();
                } else {
                    log.error("Could not find collection in ACE: {} - {}", response.code(), response.message());
                }

                phaser.arriveAndDeregister();
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("Error communicating with ACE", throwable);
                phaser.arriveAndDeregister();
            }
        });
    }

    private void setRegistered() {
        log.info("Setting replication as REGISTERED");
        Call<Replication> update = ingest.updateReplicationStatus(replication.getId(),
                new RStatusUpdate(ReplicationStatus.ACE_REGISTERED));
        update.enqueue(new UpdateCallback());
    }

    @Override
    public Long call() throws Exception {
        if (id == -1) {
            run();
        }

        return id;
    }
}
