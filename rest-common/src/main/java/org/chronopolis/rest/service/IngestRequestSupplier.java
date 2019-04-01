package org.chronopolis.rest.service;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.chronopolis.rest.models.create.BagCreate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Class to gather metadata about a bag and supply an IngestRequest for creating
 * the bag in the Ingest Server
 * <p>
 * todo: passing in the path of the bag feels a bit clunky. maybe we could have it so we
 * only need the stage, depositor, and name
 * <p>
 * Created by shake on 7/18/17.
 */
public class IngestRequestSupplier implements Supplier<Optional<BagCreate>> {
    private final Logger log = LoggerFactory.getLogger(IngestRequestSupplier.class);
    private final String TAR_TYPE = "application/x-tar";

    private Path bag;
    private final Path stage;
    private final String name;
    private final String depositor;

    public IngestRequestSupplier(Path bag, Path stage, String depositor, String name) {
        this.bag = bag;
        this.stage = stage;
        this.depositor = depositor;
        this.name = name;
    }

    /**
     * Gather data about a bag and return it in the form of an IngestRequest
     * <p>
     *
     * @return The IngestRequest to push to the Ingest Server
     */
    @Override
    public Optional<BagCreate> get() {
        Optional<BagCreate> request = Optional.empty();
        if (!bag.toFile().exists()) {
            log.warn("{} not found on disk!", bag);
            return request;
        }

        try {
            // check if we should untar the bag
            log.trace("Probing mime type for {}", bag);
            String mimeType = Files.probeContentType(bag);
            if (mimeType != null && mimeType.equals(TAR_TYPE)) {
                bag = untar(bag);
            }
        } catch (IOException e) {
            log.error("Error probing mime type", e);
            return Optional.empty();
        }

        // update the path of the bag to the exploded directory
        Path relBag = stage.relativize(bag);

        try (Stream<Path> files = Files.walk(bag)) {
            request = files.map(this::fromPath)
                    .reduce(this::combineCount)
                    .map(count -> new BagCreate(name, count.size, count.files, 0L,
                            relBag.toString(), depositor));
        } catch (IOException e) {
            log.error("Error accumulating size of bag", e);
        }

        return request;
    }

    /**
     * Explode a tarball for a given transfer
     * <p>
     * TODO: Do we want a separate process for this?
     * i.e. keep this class for creating requests; move untarring to a class for... untarring
     * could have a check that says if (!bag.exists) { untar } else { init }
     *
     * @param tarball the tarball'd bag to extract
     */
    private Path untar(Path tarball) throws IOException {
        // Set up our tar stream and channel
        try (TarArchiveInputStream tais = new TarArchiveInputStream(java.nio.file.Files.newInputStream(tarball));
             ReadableByteChannel inChannel = Channels.newChannel(tais)) {
            TarArchiveEntry entry = tais.getNextTarEntry();

            // Get our root path (just the staging area), and create an updated bag path
            Path root = stage.resolve(depositor);
            Path bag = root.resolve(entry.getName());
            while (entry != null) {
                Path entryPath = root.resolve(entry.getName());

                if (entry.isDirectory()) {
                    log.trace("Creating directory {}", entry.getName());
                    java.nio.file.Files.createDirectories(entryPath);
                } else {
                    log.trace("Creating file {}", entry.getName());

                    entryPath.getParent().toFile().mkdirs();

                    // In case files are greater than 2^32 bytes, we need to use a
                    // RandomAccessFile and FileChannel to write them
                    RandomAccessFile file = new RandomAccessFile(entryPath.toFile(), "rw");
                    FileChannel out = file.getChannel();

                    // The TarArchiveInputStream automatically updates its offset as
                    // it is read, so we don't need to worry about it
                    out.transferFrom(inChannel, 0, entry.getSize());
                    out.close();
                }

                entry = tais.getNextTarEntry();
            }

            // get the directory name of the bag
            //        0       1           2
            // root = stage + depositor
            // bag  = stage + depositor + bag-name
            // root.namecount = 2
            return root.resolve(bag.getName(root.getNameCount()));
        }
    }

    /**
     * Create a count from a Path, using its length
     *
     * @param path The path to check
     * @return A count with initialized fields if the path is a file
     */
    private Count fromPath(Path path) {
        Count count = new Count();
        File file = path.toFile();
        if (file.isFile()) {
            count.size = file.length();
            count.files = 1L;
        }
        return count;
    }

    /**
     * Combine two Count classes
     * <p>
     * todo: do we really want to create a new Count instance?
     *
     * @param left  The left count argument
     * @param right The right count argument
     * @return A new Count which has combined both arguments
     */
    private Count combineCount(Count left, Count right) {
        Count count = new Count();
        count.size = left.size + right.size;
        count.files = left.files + right.files;
        return count;
    }

    /**
     * A simple class to count the total size and number of files
     */
    private static class Count {
        private long size = 0;
        private long files = 0;
    }
}
