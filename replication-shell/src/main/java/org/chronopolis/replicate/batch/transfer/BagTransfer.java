package org.chronopolis.replicate.batch.transfer;

import com.google.common.hash.HashCode;
import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.common.transfer.RSyncTransfer;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.batch.callback.UpdateCallback;
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
 * Class which transfers bags via rsync
 *
 * Created by shake on 10/11/16.
 */
public class BagTransfer implements Transfer, Runnable {
    private final Logger log = LoggerFactory.getLogger("rsync-log");

    // Fields set by constructor
    private final Bag bag;
    private final IngestAPI ingestAPI;
    private final ReplicationProperties properties;

    // These could all be local
    private final Long id;
    private final String location;
    private final String depositor;

    public BagTransfer(Replication r, IngestAPI ingestAPI, ReplicationProperties properties) {
        this.bag = r.getBag();
        this.ingestAPI = ingestAPI;
        this.properties = properties;

        this.id = r.getId();
        this.location = r.getBagLink();
        this.depositor = r.getBag().getDepositor();
    }

    @Override
    public void run() {
        ReplicationProperties.Storage storage = properties.getStorage();
        String name = bag.getName();

        // Replicate the collection
        log.info("{} Downloading bag from {}", name, location);
        Path depositorPath = Paths.get(storage.getPreservation(), depositor);
        RSyncTransfer transfer = new RSyncTransfer(location);

        try {
            Path bagPath = transfer.getFile(location, depositorPath);
            log(bag, transfer.getOutput());
            hash(bag, bagPath.resolve("tagmanifest-sha256.txt"));
        } catch (FileTransferException e) {
            log(bag, transfer.getErrors()); // ???
            log.error("{} File transfer exception", name, e);
            fail(e);
        }
    }

    @Override
    public void update(HashCode hash) {
        String calculatedDigest = hash.toString();
        log.info("{} Calculated digest {} for tagmanifest", bag.getName(), calculatedDigest);

        Call<Replication> call = ingestAPI.updateTagManifest(id, new FixityUpdate(calculatedDigest));
        call.enqueue(new UpdateCallback());
    }
}
