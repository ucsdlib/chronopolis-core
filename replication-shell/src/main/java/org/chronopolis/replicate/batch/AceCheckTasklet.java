package org.chronopolis.replicate.batch;

import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.replicate.batch.callback.UpdateCallback;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;

/**
 * @deprecated will be removed in 1.4.0-RELEASE
 * fish fish fish fish fish
 *
 * Created by shake on 3/10/16.
 */
@Deprecated
public class AceCheckTasklet implements Tasklet {
    private final Logger log = LoggerFactory.getLogger(AceCheckTasklet.class);

    private IngestAPI ingest;
    private AceService aceService;
    private Replication replication;

    public AceCheckTasklet(IngestAPI ingest, AceService aceService, Replication replication) {
        this.ingest = ingest;
        this.aceService = aceService;
        this.replication = replication;
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        Bag bag = replication.getBag();
        GetCallback callback = new GetCallback();
        Call<GsonCollection> call = aceService.getCollectionByName(bag.getName(), bag.getDepositor());
        call.enqueue(callback);
        Optional<GsonCollection> collection = callback.get();
        if (collection.isPresent()) {
            checkCollection(collection.get());
        }

        return RepeatStatus.FINISHED;
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
