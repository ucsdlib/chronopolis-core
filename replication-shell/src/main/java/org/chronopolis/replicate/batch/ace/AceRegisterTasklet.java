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
    private final Logger log = LoggerFactory.getLogger("ace-log");

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
        Bag bag = replication.getBag();
        String name = bag.getName();
        log.trace("{} Building ACE json", name);

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
        final String name = bag.getName();
        Path collectionPath = Paths.get(settings.getPreservation(),
                bag.getDepositor(),
                name);

        GsonCollection aceGson = new GsonCollection.Builder()
                .name(name)
                .digestAlgorithm("SHA-256")
                .directory(collectionPath.toString())
                .group(bag.getDepositor())
                .storage("local")
                .auditPeriod(String.valueOf(settings.getAuditPeriod()))
                .auditTokens("true")
                .proxyData("false")
                .build();

        log.debug("{} POSTing {}", name, aceGson.toJsonJackson());

        // callback register
        phaser.register();

        Call<Map<String, Long>> call = aceService.addCollection(aceGson);
        call.enqueue(new Callback<Map<String, Long>>() {
            @Override
            public void onResponse(Call<Map<String, Long>> call, Response<Map<String, Long>> response) {
                if (response.isSuccessful()) {
                    id = response.body().get("id");
                    setRegistered();
                } else {
                    log.error("{} Error registering collection in ACE: {} - {}", new Object[]{name, response.code(), response.message()});
                    try {
                        log.debug("{} {}", name, response.errorBody().string());
                    } catch (IOException ignored) {
                    }

                    notifier.setSuccess(false);
                }

                // arrive and dereg?
                phaser.arrive();
            }

            @Override
            public void onFailure(Call<Map<String, Long>> call, Throwable throwable) {
                log.error("{} Error communicating with ACE", name, throwable);
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
            public void onResponse(Call<GsonCollection> call, Response<GsonCollection> response) {
                if (response.isSuccessful() && response.body() != null) {
                    id = response.body().getId();
                } else {
                    log.info("{} not found in ACE, attempting to register", bag.getName());
                }

                phaser.arriveAndDeregister();
            }

            @Override
            public void onFailure(Call<GsonCollection> call, Throwable throwable) {
                log.error("{} Error communicating with ACE", bag.getName(), throwable);
                phaser.arriveAndDeregister();
            }
        });
    }

    private void setRegistered() {
        log.info("{} Setting replication as REGISTERED", replication.getBag().getName());
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
