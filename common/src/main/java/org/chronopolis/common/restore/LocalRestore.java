package org.chronopolis.common.restore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Restore a collection by pulling items from local disk
 *
 * Created by shake on 7/22/14.
 */
public class LocalRestore implements CollectionRestore {
    private final Logger log = LoggerFactory.getLogger(LocalRestore.class);

    private final Path preservation;
    private final Path staging;

    public LocalRestore(Path preservation, Path staging) {
        this.preservation = preservation;
        this.staging = staging;
    }

    @Override
    public Path restore(final String depositor, final String collection) {
        Path collectionPath = preservation.resolve(depositor)
                                          .resolve(collection);

        if (!collectionPath.toFile().exists()) {
            log.error("{} not found!", collectionPath);
            return null;
        }

        try {
            // TODO: We'll probably want to break this out into its own class
            //       so that it doesn't look so crufty
            Files.walkFileTree(collectionPath, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(final Path path, final BasicFileAttributes basicFileAttributes) throws IOException {
                    Path rel = preservation.relativize(path);
                    log.trace("Making directory {}", rel);
                    Files.createDirectory(staging.resolve(rel));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(final Path path, final BasicFileAttributes basicFileAttributes) throws IOException {
                    Path rel = preservation.relativize(path);
                    log.trace("Copying file {}", rel);
                    Files.copy(path, staging.resolve(rel));
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(final Path path, final IOException e) throws IOException {
                    log.error("Error visiting file: {}", path);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path path, final IOException e) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("IOError while walking file tree {}", collectionPath, e);
            return null;
        }


        return staging.resolve(depositor).resolve(collection);
    }

}
