package org.chronopolis.replicate.batch;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.replicate.ReplicationQueue;
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
public class TokenDownloadStep implements Tasklet {
    private final Logger log = LoggerFactory.getLogger(TokenDownloadStep.class);

    private ReplicationSettings settings;
    private CollectionInitMessage message;

    public TokenDownloadStep(final ReplicationSettings settings,
                             final CollectionInitMessage message) {
        this.settings = settings;
        this.message = message;
    }


    @Override
    public RepeatStatus execute(final StepContribution stepContribution, final ChunkContext chunkContext) throws Exception {
        String location = message.getTokenStore();
        String protocol = message.getProtocol();
        String digest = message.getTokenStoreDigest();

        log.info("Downloading Token Store from {}", location);
        Path manifest = null;
        try {
            manifest = ReplicationQueue.getFileImmediate(location,
                    Paths.get(settings.getPreservation()),
                    protocol);
        } catch (IOException e) {
            log.error("Error downloading token store", e);
        } catch (FileTransferException e) {
            log.error("File transfer exception", e);
        }

        HashFunction hashFunction = Hashing.sha256();
        HashCode hashCode = null;
        try {
            hashCode = Files.hash(manifest.toFile(), hashFunction);
        } catch (IOException e) {
            log.error("Error hashing token store", e);
        }

        String calculatedDigest = hashCode.toString();
        log.trace("Calculated digest {} for token store", calculatedDigest);

        if (!calculatedDigest.equalsIgnoreCase(digest)) {
            log.error("Downloaded token store does not match expected digest!" +
                            "\nFound {}\nExpected {}",
                    calculatedDigest,
                    digest);
        } else {
            log.info("Successfully validated token store");
        }

        return RepeatStatus.FINISHED;
    }
}
