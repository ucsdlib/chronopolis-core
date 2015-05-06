package org.chronopolis.intake.processor;

import com.google.common.collect.Sets;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionRestoreCompleteMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

import static org.chronopolis.messaging.Indicator.ACK;

/**
 * Created by shake on 7/17/14.
 */
@Deprecated
public class CollectionRestoreCompleteProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(CollectionRestoreCompleteProcessor.class);

    private Set<String> accepted = Sets.newHashSet(
            ".collection-snapshot.properties",
            "manifest-md5.txt",
            "manifest-sha256.txt",
            "content-properties.json");

    private static final String DATA_DIR = "data";

    @Autowired
    private IntakeSettings intakeSettings;

    @Override
    public void process(final ChronMessage chronMessage) {
        if (!(chronMessage instanceof CollectionRestoreCompleteMessage)) {
            throw new RuntimeException("Unexpected message type " + chronMessage.getType());
        }

        CollectionRestoreCompleteMessage message = (CollectionRestoreCompleteMessage) chronMessage;
        Path duracloudBase = Paths.get(intakeSettings.getDuracloudSnapshotStage());

        if (ACK.name().equalsIgnoreCase(message.getMessageAtt())) {
            final Path bag = duracloudBase.resolve(message.getLocation());

            try {
                // TODO: FileVisitor will probably just be a separate class
                Files.walkFileTree(bag, new FileVisitor<Path>() {
                    @Override
                    public FileVisitResult preVisitDirectory(final Path path, final BasicFileAttributes basicFileAttributes) throws IOException {
                        if (path.endsWith(DATA_DIR)) {
                            return FileVisitResult.SKIP_SUBTREE;
                        } else {
                            return FileVisitResult.CONTINUE;
                        }
                    }

                    @Override
                    public FileVisitResult visitFile(final Path path, final BasicFileAttributes basicFileAttributes) throws IOException {
                        if (!accepted.contains(path.getFileName().toString())) {
                            log.info("Removing file {}", path.getFileName());
                            Files.delete(path);
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(final Path path, final IOException e) throws IOException {
                        // LOG
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(final Path path, final IOException e) throws IOException {
                        if (!path.equals(bag)) {
                            log.info("Removing directory {}", path.getFileName());
                            Files.delete(path);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                // LOG
                log.error("IOException curating bag for duracloud", e);
            }
        }

        // TODO: Validate files from manifest

        // TODO: notify duraspace the collection is ready (e-mail for now)
    }

}
