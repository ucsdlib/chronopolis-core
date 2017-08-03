package org.chronopolis.replicate.batch.transfer;

import com.google.common.hash.HashCode;
import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.common.storage.PreservationProperties;
import org.chronopolis.common.transfer.RSyncTransfer;
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
    private final PreservationProperties properties;

    // These could all be local
    private final Long id;
    private final String location;
    private final String depositor;

    public BagTransfer(Replication r, IngestAPI ingestAPI, PreservationProperties properties) {
        this.bag = r.getBag();
        this.ingestAPI = ingestAPI;
        this.properties = properties;

        this.id = r.getId();
        this.location = r.getBagLink();
        this.depositor = r.getBag().getDepositor();
    }

    @Override
    public void run() {
        Posix posix;
        if (properties.getPosix().isEmpty()) {
            log.error("No Preservation Storage Areas defined! Aborting!");
            throw new RuntimeException("No Preservation Storage Areas defined! Aborting!");
        } else {
            // todo: logic to pick which area to get
            posix = properties.getPosix().get(0); // just get the head for now
        }
        String name = bag.getName();

        // Replicate the collection
        log.info("{} Downloading bag from {}", name, location);
        Path depositorPath = Paths.get(posix.getPath(), depositor);
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
