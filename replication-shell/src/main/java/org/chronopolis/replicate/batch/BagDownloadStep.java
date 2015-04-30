package org.chronopolis.replicate.batch;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.common.transfer.HttpsTransfer;
import org.chronopolis.common.transfer.RSyncTransfer;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by shake on 8/22/14.
 */
public class BagDownloadStep implements Tasklet {
    private final Logger log = LoggerFactory.getLogger(BagDownloadStep.class);

    private ReplicationSettings settings;
    private ReplicationNotifier notifier;
    private String collection;
    private String depositor;
    private String location;
    private String protocol;

    public BagDownloadStep(final ReplicationSettings settings,
                           final CollectionInitMessage message,
                           final ReplicationNotifier notifier) {
        this.settings = settings;
        this.notifier = notifier;
        this.collection = message.getCollection();
        this.depositor = message.getDepositor();
        this.location = message.getBagLocation();
        this.protocol = message.getProtocol();
    }

    public BagDownloadStep(ReplicationSettings settings,
                           ReplicationNotifier notifier,
                           Replication replication) {
        this.settings = settings;
        this.notifier = notifier;

        Bag bag = replication.getBag();
        this.collection = bag.getName();
        this.depositor = bag.getDepositor();
        this.location = replication.getBagLink();
        this.protocol = replication.getProtocol();

        // TODO: From the rest perspective, the flow gets changed a little:
        // instead of checking against the tag digest we update the object and check if it
        // is reported as correct by the ingest service
    }

    @Override
    public RepeatStatus execute(final StepContribution stepContribution, final ChunkContext chunkContext) throws Exception {
        // For our notifier
        String statusMessage = "success";

        // Replicate the collection
        log.info("Downloading bag from {}", location);
        FileTransfer transfer;
        Path bagPath = Paths.get(settings.getPreservation(), depositor);

        String uri;
        if (protocol.equalsIgnoreCase("https")) {
            transfer = new HttpsTransfer();
            uri = location;
        } else {
            String[] parts = location.split("@", 2);
            String user = parts[0];
            uri = parts[1];
            transfer = new RSyncTransfer(user);
        }

        try {
            transfer.getFile(uri, bagPath);
            notifier.setRsyncStats(transfer.getStats());
        } catch (FileTransferException e) {
            log.error("File transfer exception", e);
            notifier.setSuccess(false);
            statusMessage = e.getMessage();
        }


        // TODO: Move duplicate code to a function somewhere
        // TODO: Verify all files?
        // Validate the tag manifest
        Path tagmanifest = bagPath.resolve(collection + "/tagmanifest-sha256.txt");
        HashFunction hashFunction = Hashing.sha256();
        HashCode hashCode = null;
        try {
            hashCode = Files.hash(tagmanifest.toFile(), hashFunction);
        } catch (IOException e) {
            log.error("Error hashing tagmanifest", e);
            statusMessage = e.getMessage();
        }

        String calculatedDigest;
        if (hashCode != null) {
            calculatedDigest = hashCode.toString();
        } else {
            calculatedDigest = "";
        }

        log.info("Calculated digest {} for tagmanifest", calculatedDigest);

        /*
        if (tagDigest.isEmpty()) {
            // update replication object
        } else {
            if (!calculatedDigest.equalsIgnoreCase(tagDigest)) {
                log.error("Downloaded tagmanifest does not match expected digest!" +
                                "\nFound {}\nExpected {}",
                        calculatedDigest,
                        tagDigest);
                statusMessage = "Downloaded tag manifest does not match expected digest";
            } else {
                log.info("Successfully validated tagmanifest");
            }
        }
        */

        notifier.setCalculatedTagDigest(calculatedDigest);
        notifier.setBagStep(statusMessage);
        return RepeatStatus.FINISHED;
    }
}
