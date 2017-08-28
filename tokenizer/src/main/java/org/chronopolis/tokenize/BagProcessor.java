package org.chronopolis.tokenize;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.common.util.Filter;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.tokenize.batch.ChronopolisTokenRequestBatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

import static com.google.common.io.Files.asByteSource;

/**
 * Processes for tokenizing a Bag
 * <p>
 * Question regarding testing: Should we put this in a package (processor maybe) and make the methods protected?
 * It would give us finer grain unit tests, as opposed to ones which span over the entire class
 * Just a thought.
 *
 * @author shake
 */
public class BagProcessor implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(BagProcessor.class);

    private final Bag bag;
    private final Digester digester;
    private final Filter<String> filter;
    private final BagStagingProperties properties;
    private final ChronopolisTokenRequestBatch batch;

    // these should come from some source above instead of being defined here
    // in the event we no longer use sha256
    private final String manifestName = "manifest-sha256.txt";
    private final String tagmanifestName = "tagmanifest-sha256.txt";


    public BagProcessor(Bag bag, Filter<String> filter, BagStagingProperties properties, ChronopolisTokenRequestBatch batch) {
        this(bag, filter, properties, batch,
                // eventually this will be cleaned up a bit when we have "storage aware" classes
                // but for now we just have posix areas sooooo yea
                Digester.of(properties.getPosix().getPath(), bag.getBagStorage().getPath()));
    }

    public BagProcessor(Bag bag,
                        Filter<String> filter,
                        BagStagingProperties properties,
                        ChronopolisTokenRequestBatch batch,
                        Digester digester) {
        this.bag = bag;
        this.digester = digester;
        this.filter = filter;
        this.properties = properties;
        this.batch = batch;
    }

    /**
     * Process a bag and Tokenize its files
     */
    @Override
    public void run() {
        String root = properties.getPosix().getPath();
        String relative = bag.getBagStorage().getPath();

        // I'm not sure of the best way to handle this, but we only want to continue
        // if we've finished processing the previous manifest. In each case this means
        // we should encounter 0 errors. Also not sure what type of strain this puts on
        // the ingest server but we'll see in testing.
        long errors = 0;
        for (String name : ImmutableList.of(manifestName, tagmanifestName)) {
            errors = process(bag, root, relative, name);

            // is there a better way to handle this?
            if (errors > 0) {
                break;
            }
        }

        if (errors == 0 && !filter.contains(tagmanifestName)) {
            Optional<String> digest = digester.digest(tagmanifestName);
            digest.ifPresent(this::processTag);
        }
    }

    /**
     * Add a tagmanifest to the batch processing with a given manifest
     *
     * @param digest the digest of the tagmanifest
     */
    private void processTag(String digest) {
        ManifestEntry tag = new ManifestEntry(bag, tagmanifestName, digest);
        // just in case this gets used down the line
        tag.setCalculatedDigest(digest);
        batch.add(tag);
    }

    /**
     * Process a given manifest to create ACE Tokens
     *
     * @param bag      the bag being processed
     * @param root     the root directory of the bag
     * @param relative the directory of the bag
     * @param name     the name of the manifest to read
     * @return the number of errors encountered
     */
    private long process(Bag bag, String root, String relative, String name) {
        long errors;
        final int PATH_IDX = 1;
        Path manifest = Paths.get(root, relative, name);
        try (Stream<String> lines = Files.lines(manifest)) {
            errors = lines.map(line -> line.split("\\s", 2))
                    .filter(entry -> !filter.contains(entry[PATH_IDX]))
                    .reduce(0, (u, entry) -> validate(entry), (l, r) -> l + r);
        } catch (IOException e) {
            log.error("[{}] Error processing {}", bag.getName(), name);
            log.error("", e);
            errors = 1;
        }

        return errors;
    }

    /**
     * Validate an entry in a manifest and add it to the batch processor if valid
     *
     * @param entry the manifest entry to add, should be of the form "digest  relative/path"
     * @return the amount of errors occurred (0 or 1)
     */
    private int validate(String[] entry) {
        int error = 1;
        final int PATH_IDX = 1;
        final int DIGEST_IDX = 0;
        ManifestEntry manifestEntry = digester.entryFrom(bag, entry[PATH_IDX].trim(), entry[DIGEST_IDX].trim());
        log.info("Creating entry from {} {}", entry[PATH_IDX].trim(), entry[DIGEST_IDX].trim());
        if (manifestEntry.isValid()) {
            batch.add(manifestEntry);
            error = 0;
        }

        return error;
    }


    public static class Digester {
        private final Path root;

        public static Digester of(String first, String... paths) {
            return new Digester(Paths.get(first, paths));
        }

        public Digester(Path root) {
            this.root = root;
        }

        /**
         * Create a ManifestEntry from a given bag, filename, and digest. Attempt
         * to calculate the digest of the file on disk.
         *
         * @param bag    the bag containing the entry
         * @param name   the filename of the entry
         * @param digest the digest of the entry
         * @return a new ManifestEntry
         */
        public ManifestEntry entryFrom(Bag bag, String name, String digest) {
            ManifestEntry entry = new ManifestEntry(bag, name, digest);
            try {
                HashCode hash = run(name);
                entry.setCalculatedDigest(hash.toString());
            } catch (IOException e) {
                log.warn("Unable to digest {}", name, e);
            }

            return entry;
        }

        /**
         * Attempt to calculate the digest of a file based on its filename
         *
         * @param name the name of the file
         * @return The digest of the file, if successful
         */
        public Optional<String> digest(String name) {
            Optional<String> digest = Optional.empty();
            try {
                HashCode hash = run(name);
                digest = Optional.of(hash.toString());
            } catch (IOException e) {
                log.warn("Unable to digest {}", name, e);
            }

            return digest;
        }

        /**
         * Run a hashing operation on a file
         *
         * @param name the name of the file to hash
         * @return the resulting hashcode of the file
         * @throws IOException if there's an error hashing
         */
        private HashCode run(String name) throws IOException {
            return asByteSource(root.resolve(name).toFile())
                    .hash(Hashing.sha256());

        }
    }

}
