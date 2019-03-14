package org.chronopolis.replicate.batch;

import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
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

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

/**
 * fish fish fish fish fish
 * eating fish
 *
 * Created by shake on 10/13/16.
 */
public class AceCheck implements Supplier<ReplicationStatus> {
    private final Logger log = LoggerFactory.getLogger("ace-log");

    private final AceService ace;
    private final Replication replication;
    private final ReplicationService replications;

    public AceCheck(AceService ace, ServiceGenerator generator, Replication replication) {
        this.ace = ace;
        this.replication = replication;
        this.replications = generator.replications();
    }

    @Override
    public ReplicationStatus get() {
        Bag bag = replication.getBag();
        GetCallback callback = new GetCallback();
        Call<GsonCollection> call = ace.getCollectionByName(bag.getName(), bag.getDepositor());
        call.enqueue(callback);
        Optional<GsonCollection> collection = callback.get();
        return collection.map(this::checkCollection)
                .orElse(ReplicationStatus.ACE_AUDITING);
    }

    private ReplicationStatus checkCollection(GsonCollection gsonCollection) {
        ReplicationStatus current = ReplicationStatus.ACE_AUDITING;
        log.debug("{} status is {}", gsonCollection.getName(), gsonCollection.getState());

        // TODO: Check for errors as well
        if (gsonCollection.getState().equals("A")) {
            current = ReplicationStatus.SUCCESS;
            Call<Replication> call = replications.updateStatus(replication.getId(),
                    new ReplicationStatusUpdate(current));
            call.enqueue(new UpdateCallback());
        }

        return current;
    }

    class GetCallback implements Callback<GsonCollection> {

        GsonCollection collection;
        final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void onResponse(@NotNull Call<GsonCollection> call,
                               @NotNull Response<GsonCollection> response) {
            if (response.isSuccessful()) {
                collection = response.body();
            } else {
                collection = null;
            }
            latch.countDown();
        }

        @Override
        public void onFailure(@NotNull Call<GsonCollection> call,
                              @NotNull Throwable t) {
            collection = null;
            latch.countDown();
        }

        public Optional<GsonCollection> get() {
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }

            return Optional.ofNullable(collection);
        }
    }
}
