package org.chronopolis.replicate.batch.transfer;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.replicate.ReplicationQueue;
import org.chronopolis.replicate.batch.callback.UpdateCallback;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.FixityUpdate;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class to transfer token stores via rsync
 *
 * Created by shake on 10/11/16.
 */
public class TokenTransfer implements Runnable {

    private final Logger log = LoggerFactory.getLogger("rsync-log");

    // Set in our constructor
    final Bag b;
    final Replication r;
    final IngestAPI ingest;
    final ReplicationSettings settings;

    // Set when running
    final Long id;
    final String location;
    final String protocol;
    final String depositor;

    public TokenTransfer(Replication r, IngestAPI ingest, ReplicationSettings settings) {
        this.r = r;
        this.b = r.getBag();
        this.ingest = ingest;
        this.settings = settings;

        this.id = r.getId();
        this.protocol = r.getProtocol();
        this.location = r.getTokenLink();
        this.depositor = r.getBag().getDepositor();
    }

    @Override
    public void run() {
        String name = b.getName();
        log.info("{} Downloading Token Store from {}", name, location);

        Path tokenStore;

        // Make sure the directory for the depositor exists before pulling
        Path stage = Paths.get(settings.getPreservation(), depositor);
        checkDirExists(stage);

        try {
            // todo: move off of this
            tokenStore = ReplicationQueue.getFileImmediate(
                    location,
                    stage,
                    protocol);
            hash(tokenStore);
        } catch (IOException e) {
            log.error("{} Error downloading token store", name,  e);
            fail(e);
        } catch (FileTransferException e) {
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

    void hash(Path token) {
        HashFunction hashFunction = Hashing.sha256();
        HashCode hash;
        try {
            // Check to make sure the download was successful
            if (!token.toFile().exists()) {
                throw new IOException("TokenStore "
                        + token.toString()
                        + " does does not exist");
            }

            hash = Files.hash(token.toFile(), hashFunction);
            update(hash);
        } catch (IOException e) {
            log.error("{} Error hashing token store", b.getName(), e);
            fail(e);
        }
    }

    void update(HashCode hash) {
        String calculatedDigest = hash.toString();
        log.info("{} Calculated digest {} for token store", b.getName(), calculatedDigest);

        Call<Replication> call = ingest.updateTokenStore(id, new FixityUpdate(calculatedDigest));
        call.enqueue(new UpdateCallback());
    }

    void fail(Exception e) {
        throw new RuntimeException(e);
    }

}
