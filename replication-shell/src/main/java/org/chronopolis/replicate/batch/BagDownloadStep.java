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
import org.chronopolis.replicate.config.ReplicationSettings;
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
    private CollectionInitMessage message;

    public BagDownloadStep(final ReplicationSettings settings,
                           final CollectionInitMessage message) {
        this.settings = settings;
        this.message = message;
    }

    @Override
    public RepeatStatus execute(final StepContribution stepContribution, final ChunkContext chunkContext) throws Exception {
        // Set up our download parameters
        String collection = message.getCollection();
        String depositor = message.getDepositor();
        String location = message.getBagLocation();
        String protocol = message.getProtocol();
        String tagDigest = message.getTagManifestDigest();

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
        } catch (FileTransferException e) {
            log.error("File transfer exception", e);
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
        }

        String calculatedDigest = hashCode.toString();
        log.trace("Calculated digest {} for tagmanifest", calculatedDigest);

        if (!calculatedDigest.equalsIgnoreCase(tagDigest)) {
            log.error("Downloaded tagmanifest does not match expected digest!" +
                      "\nFound {}\nExpected {}",
                    calculatedDigest,
                    tagDigest);
        } else {
            log.info("Successfully validated tagmanifest");
        }


        return RepeatStatus.FINISHED;
    }
}
