package org.chronopolis.common.ace;

import com.google.common.hash.HashCode;
import edu.umiacs.ace.ims.api.IMSService;
import edu.umiacs.ace.ims.api.RequestBatchCallback;
import edu.umiacs.ace.ims.api.TokenRequestBatch;
import edu.umiacs.ace.ims.ws.TokenRequest;
import org.chronopolis.common.digest.Digest;
import org.chronopolis.common.util.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Class to create ACE tokens from a BagIt bag.
 * Reads in the manifest and tagmanifest for entries
 * to use.
 *
 * Created by shake on 2/4/15.
 */
public class Tokenizer {

    /**
     * Factory class to allow for mocking of IMS connections
     *
     */
    public static class IMSFactory {
        public TokenRequestBatch createIMSConnection(String imsHostName, RequestBatchCallback callback) {
            IMSService ims;
            // TODO: Use the AceSettings to get the ims host name
            ims = IMSService.connect(imsHostName, SSL_PORT, true);
            return ims.createImmediateTokenRequestBatch("SHA-256",
                    callback,
                    MAX_QUEUE_LEN,
                    TIMEOUT);
        }
    }

    private static final int SSL_PORT = 443;
    private static final int MAX_QUEUE_LEN = 1000;
    private static final int TIMEOUT = 5000;

    private final Logger log = LoggerFactory.getLogger(Tokenizer.class);
    private final Path bag;
    private final String imsHostName;

    private final Digest fixityAlgorithm;

    // What the tagmanifest looks like in our filter
    private Path aceTag;
    private Path manifest;
    private Path tagmanifest;
    private String tagIdentifier;
    private String tagDigest;
    private Set<Path> extraTagmanifests;

    private final RequestBatchCallback callback;
    private TokenRequestBatch batch;

    private IMSFactory factory;

    public Tokenizer(final Path bag,
                     final String fixityAlgorithm,
                     final String imsHostName,
                     final RequestBatchCallback callback) {
        this.bag = bag;
        this.fixityAlgorithm = Digest.fromString(fixityAlgorithm);
        this.extraTagmanifests = new HashSet<>();
        this.callback = callback;
        this.tagDigest = null;
        this.factory = new Tokenizer.IMSFactory();
        this.imsHostName = imsHostName;
        addManifests();
    }

    public Tokenizer(final Path bag,
                     final String fixityAlgorithm,
                     final String imsHostName,
                     final RequestBatchCallback callback,
                     IMSFactory imsFactory) {
        this.bag = bag;
        this.fixityAlgorithm = Digest.fromString(fixityAlgorithm);
        this.extraTagmanifests = new HashSet<>();
        this.callback = callback;
        this.tagDigest = null;
        this.factory = imsFactory;
        this.imsHostName = imsHostName;
        addManifests();
    }

    private void addManifests() {
        tagIdentifier = "tagmanifest-"
                + fixityAlgorithm.getBagitIdentifier()
                + ".txt";

        Path tagManifest = bag.resolve(tagIdentifier);
        Path manifest = bag.resolve("manifest-"
                + fixityAlgorithm.getBagitIdentifier()
                + ".txt");

        // ACE holds files in a relative context, without the preceding directories
        Path aceTag = Paths.get("/tagmanifest-"
                + fixityAlgorithm.getBagitIdentifier()
                + ".txt");

        if (!tagManifest.toFile().exists()) {
            log.error("Could not find tag manifest at {}", tagManifest);
            throw new RuntimeException("TagManifest does not exist!");
        }
        if (!manifest.toFile().exists()) {
            log.error("Could not find manifest at {}", manifest);
            throw new RuntimeException("Manifest does not exist!");
        }

        this.aceTag = aceTag;
        this.manifest = manifest;
        this.tagmanifest = tagManifest;

        scanForExtra();
    }

    private void scanForExtra() {
        String identifier = fixityAlgorithm.getBagitIdentifier();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bag, new TagFilter(identifier))) {
            for (Path path : stream) {
                log.debug("Adding extra file {}", path);
                extraTagmanifests.add(path);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not read bag directory");
        }
    }

    /**
     * Create tokens for a bag based on the manifest and tagmanifest
     * TODO: Digest the tagmanifest first, then the manifest
     *
     * @param filter Set of paths to exclude from tokenization
     * @throws IOException
     * @throws InterruptedException
     */
    public void tokenize(Filter<Path> filter) throws IOException, InterruptedException {
        batch = factory.createIMSConnection(imsHostName, callback);

        try {
            // Digest the manifest
            boolean corrupt = tokenize(filter, manifest);
            if (corrupt) {
                log.error("Error(s) found in manifest, skipping it until all are corrected");
                filter.add(manifest);
                filter.add(tagmanifest); // Make sure we don't create a request for the tagmanifest
            }

            // Then the tag-manifest
            tokenize(filter, tagmanifest);

            // Digest any extra tagmanifests
            for (Path tag: extraTagmanifests) {
                Path rel = tag.subpath(bag.getNameCount(), tag.getNameCount());
                Path filterPath = Paths.get("/").resolve(rel);
                if (!filter.contains(filterPath)) {
                    String digest = calculateDigest(tag);
                    addTokenRequest(tag, digest);
                    filter.add(filterPath);
                }
            }
        } finally {
            // Make sure the batch gets closed if we are interrupted
            batch.close();
        }
    }

    /**
     * The bulk of the tokenization process. We read the given manifest, and
     * process the entries as they come in. As we digest each file in the manifest,
     * if the two digests match we create a token request (for ACE).
     *
     *
     * @param filter A set of paths we have already digested
     * @param manifest The manifest to read
     * @return true if there are errors in the manifest, false otherwise
     * @throws IOException
     * @throws InterruptedException
     */
    private boolean tokenize(Filter<Path> filter, Path manifest) throws IOException,
            InterruptedException {
        String line;
        boolean corrupt = false;

        try (BufferedReader br = Files.newBufferedReader(manifest, Charset.defaultCharset())) {
            while ((line = br.readLine()) != null) {
                String[] split = line.split("\\s+", 2);
                if (split.length != 2) {
                    log.error("Error found in manifest: {}", split);
                    continue;
                }

                String digest = split[0];
                String filePath = split[1];
                Path ace = Paths.get("/");
                Path rel = Paths.get(filePath);

                // Skip the current item if we already have it
                // use a leading slash as our ace_tokens have it as well
                if (filter.contains(ace.resolve(rel))) {
                    continue;
                }

                Path path = Paths.get(bag.toString(), filePath);
                String calculatedDigest = calculateDigest(path);

                if (digest.equals(calculatedDigest)) {
                    addTokenRequest(path, digest);
                    filter.add(ace.resolve(rel));
                } else {
                    log.error("Error in file {}: digest found {} (expected {})",
                            new Object[]{
                                    filePath,
                                    calculatedDigest,
                                    digest});
                    corrupt = true;
                }
            }
        }

        // No corruptions (all manifests good)
        // Skip the manifest
        // Skip if we've already digested the tag manifest (tokenizer gets called multiple times)
        if (!filter.contains(aceTag)) {
            if (!corrupt && manifest.getFileName().endsWith(tagIdentifier)) {
                tagDigest = calculateDigest(manifest);
                addTokenRequest(manifest, tagDigest);
            }
        }

        return corrupt;
    }

    public String getTagManifestDigest() {
        return tagDigest;
    }

    /**
     * Calculate the digest for a given file
     *
     * @param path the path of the file
     * @return the digest of the file
     * @throws IOException exception hashing the file
     */
    private String calculateDigest(Path path) throws IOException {
        HashCode hash = com.google.common.io.Files.asByteSource(path.toFile())
                .hash(fixityAlgorithm.getFunction());
        return hash.toString();
    }

    /**
     * Add an ACE TokenRequest to the {@link RequestBatchCallback}
     *
     * @param path The path of the file to add
     * @param digest The digest of the file
     * @throws InterruptedException
     */
    private void addTokenRequest(Path path, String digest) throws InterruptedException {
        Path rel = path.subpath(bag.getNameCount(), path.getNameCount());

        // ACE requires a leading /, so let's make sure we get that in the token request
        Path ace = Paths.get("/", rel.toString());
        TokenRequest req = new TokenRequest();
        req.setHashValue(digest);
        req.setName(ace.toString());
        batch.add(req);
    }

    private class TagFilter implements DirectoryStream.Filter<Path> {
        // Use a negative look ahead with a word boundary to exclude certain words
        private final String regex = "tagmanifest-(?!\\b%s\\b)\\w+.txt";

        private final Pattern pattern;

        private TagFilter(String tagType) {
            Formatter f = new Formatter();
            this.pattern = Pattern.compile(f.format(regex, tagType).toString());
        }

        @Override
        public boolean accept(Path path) throws IOException {
            // We don't care about using what was found, just if it exists
            return pattern.matcher(path.getFileName().toString()).find();
        }
    }

}
