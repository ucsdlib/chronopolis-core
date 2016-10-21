package org.chronopolis.replicate.batch;

import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.replicate.batch.callback.UpdateCallback;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.models.ReplicationStatus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * fish fish fish fish fish
 * eating fish
 *
 * Created by shake on 10/13/16.
 */
public class AceCheck implements Runnable {

    final AceService ace;
    final IngestAPI ingest;
    final Replication replication;

    public AceCheck(AceService ace, IngestAPI ingest, Replication replication) {
        this.ace = ace;
        this.ingest = ingest;
        this.replication = replication;
    }

    @Override
    public void run() {
        Bag bag = replication.getBag();
        GetCallback callback = new GetCallback();
        Call<GsonCollection> call = ace.getCollectionByName(bag.getName(), bag.getDepositor());
        call.enqueue(callback);
        Optional<GsonCollection> collection = callback.get();
        if (collection.isPresent()) {
            checkCollection(collection.get());
        }

    }

    private void checkCollection(GsonCollection gsonCollection) {
        // TODO: Check for errors as well
        if (gsonCollection.getState() == 65) {
            Call<Replication> call = ingest.updateReplicationStatus(replication.getId(), new RStatusUpdate(ReplicationStatus.SUCCESS));
            call.enqueue(new UpdateCallback());
        }
    }

    class GetCallback implements Callback<GsonCollection> {

        GsonCollection collection;
        CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void onResponse(Response<GsonCollection> response) {
            if (response.isSuccess()) {
                collection = response.body();
            } else {
                collection = null;
            }
            latch.countDown();
        }

        @Override
        public void onFailure(Throwable throwable) {
            collection = null;
            latch.countDown();
        }

        public Optional<GsonCollection> get() {
            try {
                latch.await();
            } catch (InterruptedException ignored) {
            }

            return Optional.of(collection);
        }
    }
}
