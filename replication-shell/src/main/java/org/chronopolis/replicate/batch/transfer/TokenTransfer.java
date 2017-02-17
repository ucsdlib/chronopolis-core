package org.chronopolis.replicate.batch.transfer;

import com.google.common.hash.HashCode;
import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.common.transfer.RSyncTransfer;
import org.chronopolis.replicate.batch.callback.UpdateCallback;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.FixityUpdate;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class to transfer token stores via rsync
 *
 * Created by shake on 10/11/16.
 */
public class TokenTransfer implements Transfer, Runnable {

    private final Logger log = LoggerFactory.getLogger("rsync-log");

    // Set in our constructor
    private final Bag bag;
    private final IngestAPI ingest;
    private final ReplicationSettings settings;

    // These could all be local
    private final Long id;
    private final String location;
    private final String depositor;

    public TokenTransfer(Replication r, IngestAPI ingest, ReplicationSettings settings) {
        this.bag = r.getBag();
        this.ingest = ingest;
        this.settings = settings;

        this.id = r.getId();
        this.location = r.getTokenLink();
        this.depositor = r.getBag().getDepositor();
    }

    @Override
    public void run() {
        String name = bag.getName();
        log.info("{} Downloading Token Store from {}", name, location);

        Path tokenStore;

        // Make sure the directory for the depositor exists before pulling
        Path stage = Paths.get(settings.getPreservation(), depositor);
        checkDirExists(stage);

        RSyncTransfer transfer = new RSyncTransfer(location);

        try {
            tokenStore = transfer.getFile(location, stage);
            log(bag, transfer.getOutput());
            hash(bag, tokenStore);
        } catch (FileTransferException e) {
            log(bag, transfer.getErrors());
            log.error("{} File transfer exception", name, e);
            fail(e);
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkDirExists(Path stage) {
        if (!stage.toFile().exists()) {
            stage.toFile().mkdirs();
        }
    }

    @Override
    public void update(HashCode hash) {
        String calculatedDigest = hash.toString();
        log.info("{} Calculated digest {} for token store", bag.getName(), calculatedDigest);

        Call<Replication> call = ingest.updateTokenStore(id, new FixityUpdate(calculatedDigest));
        call.enqueue(new UpdateCallback());
    }

}
