package org.chronopolis.intake.processor;

import com.google.common.collect.Sets;
import org.chronopolis.intake.duracloud.config.IntakeSettings;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionRestoreCompleteMessage;
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
public class CollectionRestoreReplyProcessor implements ChronProcessor {

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
        Path duracloudBase = Paths.get(intakeSettings.getDuracloudStage());

        if (ACK.name().equals(message.getMessageAtt())) {
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
                            Files.delete(path);
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } catch (IOException e) {
                // LOG
                System.out.println(e);
            }
        }

        // TODO: notify duraspace the collection is ready

    }

}
