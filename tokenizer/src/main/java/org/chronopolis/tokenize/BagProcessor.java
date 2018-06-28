package org.chronopolis.tokenize;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;
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
    private final TokenWorkSupervisor supervisor;
    private final BagStagingProperties properties;
    private final Predicate<ManifestEntry> predicate;

    // these should come from some source instead of being defined here
    // in the event we no longer use sha256
    @SuppressWarnings("FieldCanBeLocal")
    private final String manifestName = "manifest-sha256.txt";
    private final String tagmanifestName = "tagmanifest-sha256.txt";


    public BagProcessor(Bag bag,
                        Collection<Predicate<ManifestEntry>> predicates,
                        BagStagingProperties properties,
                        TokenWorkSupervisor supervisor) {
        this(bag, predicates, properties,
                // eventually this will be cleaned up a bit when we have "storage aware" classes
                // but for now we just have posix areas sooooo yea
                Digester.of(properties.getPosix().getPath(),
                            bag.getBagStorage().getPath()),
                supervisor);
    }

    public BagProcessor(Bag bag,
                        Collection<Predicate<ManifestEntry>> predicates,
                        BagStagingProperties properties,
                        Digester digester,
                        TokenWorkSupervisor supervisor) {
        this.bag = bag;
        this.supervisor = supervisor;
        this.digester = digester;
        this.properties = properties;

        // Just use the HttpFilter for now, soon we'll pass in a list of predicates
        this.predicate = buildPredicate(predicates);
    }

    /**
     * Create a Predicate from combining multiple Predicates together through a reduction
     *
     * @param predicates the Predicates to reduce
     * @return the reduced Predicate, or true if none are present
     */
    private <E> Predicate<E> buildPredicate(Collection<Predicate<E>> predicates) {
        return predicates.stream()
            .reduce(Predicate::and).orElse(entry -> true);
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
            log.info("[{}] Finished processing. {} Errors", bag.getName() + "/" + name, errors);

            // is there a better way to handle this?
            if (errors > 0) {
                break;
            }
        }

        if (errors == 0) {
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
        ManifestEntry entry = new ManifestEntry(bag, tagmanifestName, digest);
        if (predicate.test(entry)) {
            // just in case this gets used down the line
            supervisor.start(entry);
        }
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
        final int DIGEST_IDX = 0;
        String identifier = bag.getDepositor() + "::" + bag.getName();

        log.debug("[{}] Processing {}", identifier, name);
        Path manifest = Paths.get(root, relative, name);
        try (Stream<String> lines = Files.lines(manifest)) {
            // todo: could filter on entry.length == 2 just in case something happens
            //       otherwise we probably want to increment the error counter but idk
            // maybe instead of errors being a long, it could be a set which contains all
            // error'd entries
            errors = lines.map(line -> line.split("\\s", 2))
                    .map(entry -> new ManifestEntry(bag,
                            entry[PATH_IDX].trim(),
                            entry[DIGEST_IDX].trim()))
                    .peek(entry -> log.trace("[{}] Processing", entry.tokenName()))
                    .filter(predicate)
                    .reduce(0, (u, entry) -> validate(entry), (l, r) -> l + r);
        } catch (IOException e) {
            log.error("[{}] Error processing {}", bag.getName(), name);
            log.error("", e);
            errors = 1;
        }

        return errors;
    }

    /**
     * Validate an entry in a manifest and add it to processing if it passes validation
     *
     * @param entry the entry to validate
     * @return the number of errors found (0 or 1)
     */
    private int validate(ManifestEntry entry) {
        int error = 1;
        Optional<String> digest = digester.digest(entry.getPath());
        Boolean isValid = digest.map(d -> entry.getDigest().equalsIgnoreCase(d)).orElse(false);
        if (isValid) {
            log.info("[{}] Entry is valid", entry.tokenName());
            error = 0;
            supervisor.start(entry);
        } else {
            log.warn("[{}] Entry is invalid", entry.tokenName());
        }

        return error;
    }

    /**
     * Class to digest a file for us. Might be turned into a Future which we can submit to a
     * ThreadPool for long IO operations
     */
    @SuppressWarnings("WeakerAccess")
    public static class Digester {
        private final Path root;

        public static Digester of(String first, String... paths) {
            return new Digester(Paths.get(first, paths));
        }

        public Digester(Path root) {
            this.root = root;
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
            log.trace("digesting {}", name);
            return asByteSource(root.resolve(name).toFile())
                    .hash(Hashing.sha256());

        }
    }

}
