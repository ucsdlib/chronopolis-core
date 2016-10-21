package org.chronopolis.replicate.batch.transfer;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.common.transfer.HttpsTransfer;
import org.chronopolis.common.transfer.RSyncTransfer;
import org.chronopolis.replicate.batch.callback.UpdateCallback;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.FixityUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class which transfers bags via rsync
 *
 * Created by shake on 10/11/16.
 */
public class BagTransfer implements Runnable {
    private final Logger log = LoggerFactory.getLogger(BagTransfer.class);

    // Fields set by constructor
    final Replication r;
    final IngestAPI ingestAPI;
    final ReplicationSettings settings;

    // Fields set when running
    final Long id;
    final String location;
    final String protocol;
    final String depositor;
    final String collection;

    public BagTransfer(Replication r, IngestAPI ingestAPI, ReplicationSettings settings) {
        this.r = r;
        this.ingestAPI = ingestAPI;
        this.settings = settings;

        this.id = r.getId();
        this.location = r.getBagLink();
        this.protocol = r.getProtocol();
        this.depositor = r.getBag().getDepositor();
        this.collection = r.getBag().getName();
    }

    @Override
    public void run() {
        // TODO: Get the replication so we can short circuit later?
        // Replicate the collection
        log.info("Downloading bag from {}", location);
        FileTransfer transfer;
        Path bagPath = Paths.get(settings.getPreservation(), depositor);

        if (protocol.equalsIgnoreCase("https")) {
            transfer = new HttpsTransfer();
        } else {
            transfer = new RSyncTransfer(location);
        }

        try {
            transfer.getFile(location, bagPath);
            hash(bagPath);
        } catch (FileTransferException e) {
            log.error("File transfer exception", e);
            fail(e);
        }
    }

    void hash(Path bagPath) {
        // TODO: Verify all files?
        // Validate the tag manifest
        Path tagmanifest = bagPath.resolve(collection + "/tagmanifest-sha256.txt");
        HashFunction hashFunction = Hashing.sha256();
        HashCode hashCode;
        try {
            hashCode = Files.hash(tagmanifest.toFile(), hashFunction);
            sendUpdate(hashCode);
        } catch (IOException e) {
            log.error("Error hashing tagmanifest", e);
            fail(e);
        }
    }

    void sendUpdate(HashCode hashCode) {
        String calculatedDigest = hashCode.toString();
        log.info("Calculated digest {} for tagmanifest", calculatedDigest);

        Call<Replication> call = ingestAPI.updateTagManifest(id, new FixityUpdate(calculatedDigest));
        call.enqueue(new UpdateCallback());
    }

    void fail(Throwable throwable) {
        throw new RuntimeException(throwable);
    }
}
