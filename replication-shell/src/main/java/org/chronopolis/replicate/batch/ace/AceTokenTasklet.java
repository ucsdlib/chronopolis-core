package org.chronopolis.replicate.batch.ace;

import com.google.common.io.ByteSource;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.SingleFileOperation;
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
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runnable task to load an ACE Token Store to an ACE AM instance
 *
 * Created by shake on 3/8/16.
 */
public class AceTokenTasklet implements Runnable {
    private final Logger log = LoggerFactory.getLogger("ace-log");

    private final Bucket bucket;
    private final AceService aceService;
    private final Replication replication;
    private final SingleFileOperation operation;
    private final ReplicationService replications;
    private final ReplicationNotifier notifier;

    private final Long id;

    public AceTokenTasklet(Bucket bucket,
                           SingleFileOperation operation,
                           ServiceGenerator generator,
                           AceService aceService,
                           Replication replication,
                           ReplicationNotifier notifier,
                           Long id) {
        this.id = id;
        this.bucket = bucket;
        this.notifier = notifier;
        this.operation = operation;
        this.aceService = aceService;
        this.replication = replication;
        this.replications = generator.replications();
    }

    @Override
    public void run() {
        // Short circuit this
        if (replication.getStatus() == ReplicationStatus.ACE_TOKEN_LOADED
                || replication.getStatus() == ReplicationStatus.ACE_AUDITING) {
            return;
        }

        Bag bag = replication.getBag();
        final String name = bag.getName();

        // might be able to pull this from the storageOp
        String manifest = bag.getTokenStorage().getPath();
        log.info("{} loadTokenStore params = ({}, {})", name, id, manifest);

        // For a a Token Operation, the operation path contains the
        // full path to the token store so we join it with an empty path
        Optional<ByteSource> stream = bucket.stream(operation, operation.getFile());
        stream.map(source -> aceService.loadTokenStore(id, new AceTokenBody(source)))
                .ifPresent(call -> attemptLoad(call, name));
    }

    /**
     * Run a given call, presumably for loading a token store to an ACE-AM instance
     *
     * @param call the call to execute
     */
    private void attemptLoad(Call<Void> call, String name) {
        final AtomicBoolean complete = new AtomicBoolean(false);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NotNull Call<Void> call,
                                   @NotNull Response<Void> response) {
                final int code = response.code();
                final String message = response.message();

                if (response.isSuccessful()) {
                    log.info("{} loaded token store", name);
                    Call<Replication> update = replications.updateStatus(replication.getId(),
                            new ReplicationStatusUpdate(ReplicationStatus.ACE_TOKEN_LOADED));
                    update.enqueue(new UpdateCallback());
                } else {
                    log.warn("{} Error loading token store: {}", code, message);
                    try {
                        log.debug("{} {}", name, response.errorBody().string());
                    } catch (IOException ignored) {
                    }
                    notifier.setSuccess(false);
                }

                complete.getAndSet(true);
            }

            @Override
            public void onFailure(@NotNull Call<Void> call, @NotNull Throwable throwable) {
                complete.getAndSet(true);
                notifier.setSuccess(false);
                log.warn("{} Failure loading token store", name, throwable);
            }
        });

        // Since the callback is asynchronous, we need to wait for it to complete before moving on
        waitForCallback(complete);
    }


    /**
     * Wait for an AtomicBoolean to be set to true
     *
     * @param complete the AtomicBoolean to wait on
     */
    private void waitForCallback(AtomicBoolean complete) {
        while (!complete.get()) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                log.warn("Sleep interrupted in AceTokenTasklet", e);
            }
        }
    }

    public class AceTokenBody extends RequestBody {

        private final ByteSource source;

        public AceTokenBody(ByteSource source) {
            this.source = source;
        }

        @Override
        public MediaType contentType() {
            return MediaType.parse("ASCII Text");
        }

        @Override
        public void writeTo(@NotNull BufferedSink sink) throws IOException {
            sink.writeAll(Okio.source(source.openBufferedStream()));
        }
    }
}
