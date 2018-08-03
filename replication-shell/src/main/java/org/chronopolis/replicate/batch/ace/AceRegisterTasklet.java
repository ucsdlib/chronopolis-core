package org.chronopolis.replicate.batch.ace;

import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.StorageOperation;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.batch.callback.UpdateCallback;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.chronopolis.rest.models.update.ReplicationStatusUpdate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Phaser;

/**
 * Runnable task to register a collection in ACE from a
 * given Bucket and Replication/StorageOperation
 *
 * <p>
 * Created by shake on 3/8/16.
 */
public class AceRegisterTasklet implements Callable<Long> {
    private final Logger log = LoggerFactory.getLogger("ace-log");

    private final AceService aceService;
    private final Replication replication;
    private final ReplicationService replications;
    private final AceConfiguration aceConfiguration;
    private final ReplicationNotifier notifier;

    private final Bucket bucket;
    private final StorageOperation operation;

    private Long id = -1L;
    private final Phaser phaser;

    public AceRegisterTasklet(AceService aceService,
                              Replication replication,
                              ServiceGenerator generator,
                              AceConfiguration aceConfiguration,
                              ReplicationNotifier notifier,
                              Bucket bucket,
                              StorageOperation operation) {
        this.bucket = bucket;
        this.notifier = notifier;
        this.operation = operation;
        this.aceService = aceService;
        this.replication = replication;
        this.aceConfiguration = aceConfiguration;
        this.replications = generator.replications();

        // Phaser for main thread + callback
        phaser = new Phaser();
    }

    public void run() throws Exception {
        Bag bag = replication.getBag();
        String name = bag.getName();
        log.trace("{} Building ACE json", name);

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
        GsonCollection.Builder builder = new GsonCollection.Builder()
                .name(name)
                .digestAlgorithm("SHA-256")
                .group(bag.getDepositor())
                .auditPeriod(aceConfiguration.getAuditPeriod().toString())
                .auditTokens("true")
                .proxyData("false");
        builder = bucket.fillAceStorage(operation, builder);
        GsonCollection coll = builder.build();

        log.debug("{} POSTing {}", name, coll.toJsonJackson());

        // callback register
        phaser.register();

        Call<Map<String, Long>> call = aceService.addCollection(coll);
        call.enqueue(new Callback<Map<String, Long>>() {
            @Override
            public void onResponse(@NotNull Call<Map<String, Long>> call,
                                   @NotNull Response<Map<String, Long>> response) {
                if (response.isSuccessful()) {
                    id = response.body().get("id");
                    setRegistered();
                } else {
                    log.error("{} Error registering collection in ACE: {} - {}",
                            name, response.code(), response.message());
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
            public void onFailure(@NotNull Call<Map<String, Long>> call,
                                  @NotNull Throwable throwable) {
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

        Call<GsonCollection> call =
                aceService.getCollectionByName(bag.getName(), bag.getDepositor());
        call.enqueue(new Callback<GsonCollection>() {
            @Override
            public void onResponse(@NotNull Call<GsonCollection> call,
                                   @NotNull Response<GsonCollection> response) {
                if (response.isSuccessful() && response.body() != null) {
                    id = response.body().getId();
                } else {
                    log.info("{} not found in ACE, attempting to register", bag.getName());
                }

                phaser.arriveAndDeregister();
            }

            @Override
            public void onFailure(@NotNull Call<GsonCollection> call,
                                  @NotNull Throwable throwable) {
                log.error("{} Error communicating with ACE", bag.getName(), throwable);
                phaser.arriveAndDeregister();
            }
        });
    }

    private void setRegistered() {
        log.info("{} Setting replication as REGISTERED", replication.getBag().getName());
        Call<Replication> update = replications.updateStatus(replication.getId(),
                new ReplicationStatusUpdate(ReplicationStatus.ACE_REGISTERED));
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
