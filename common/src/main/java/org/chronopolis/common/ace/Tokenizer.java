package org.chronopolis.common.ace;

import edu.umiacs.ace.ims.api.IMSService;
import edu.umiacs.ace.ims.api.RequestBatchCallback;
import edu.umiacs.ace.ims.api.TokenRequestBatch;
import edu.umiacs.ace.ims.ws.TokenRequest;
import org.chronopolis.common.digest.Digest;
import org.chronopolis.common.digest.DigestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Class to create ACE tokens from a BagIt bag.
 * Reads in the manifest and tagmanifest for entries
 * to use.
 *
 * Created by shake on 2/4/15.
 */
public class Tokenizer {
    private static final int SSL_PORT = 443;
    private static final int MAX_QUEUE_LEN = 1000;
    private static final int TIMEOUT = 5000;

    private final Logger log = LoggerFactory.getLogger(Tokenizer.class);
    private final Path bag;

    private final Digest fixityAlgorithm;
    private Path manifest;
    private Path tagmanifest;
    private String tagIdentifier;
    private String tagDigest;

    private final RequestBatchCallback callback;
    private TokenRequestBatch batch;

    public Tokenizer(final Path bag,
                     final String fixityAlgorithm,
                     final RequestBatchCallback callback) {
        this.bag = bag;
        this.fixityAlgorithm = Digest.fromString(fixityAlgorithm);
        this.callback = callback;
        this.tagDigest = null;
        addManifests();
    }

    private void addManifests() {
        tagIdentifier = "tagmanifest-"
                + fixityAlgorithm.getBagitIdentifier()
                + ".txt";

        Path tagManifest = bag.resolve("tagmanifest-"
                + fixityAlgorithm.getBagitIdentifier()
                + ".txt");
        Path manifest = bag.resolve("manifest-"
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

        this.manifest = manifest;
        this.tagmanifest = tagManifest;
    }

    /**
     * Create tokens for a bag based on the manifest and tagmanifest
     * TODO: Digest the tagmanifest first, then the manifest
     *
     * @param filter Set of paths to exclude from tokenization
     * @throws IOException
     * @throws InterruptedException
     */
    public void tokenize(Set<Path> filter) throws IOException, InterruptedException {
        batch = createIMSConnection();

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
    private boolean tokenize(Set<Path> filter, Path manifest) throws IOException,
            InterruptedException {
        String line;
        boolean corrupt = false;
        String alg = fixityAlgorithm.getName();
        BufferedReader br = Files.newBufferedReader(manifest, Charset.defaultCharset());

        while ((line = br.readLine()) != null) {
            String[] split = line.split("\\s+", 2);
            if (split.length != 2) {
                log.error("Error found in manifest: {}", split);
                continue;
            }

            String digest = split[0];
            String filePath = split[1];
            Path rel = Paths.get(filePath);
            // Skip the current item if we already have it
            if (filter.contains(rel)) {
                continue;
            }

            Path path = Paths.get(bag.toString(), filePath);
            String calculatedDigest = DigestUtil.digest(path, alg);

            if (digest.equals(calculatedDigest)) {
                addTokenRequest(path, digest);
            } else {
                log.error("Error in file {}: digest found {} (expected {})",
                        new Object[]{
                                filePath,
                                calculatedDigest,
                                digest});
                corrupt = true;
            }
        }

        // TODO: Move this into the public method instead
        // No corruptions (all manifests good)
        // Skip the manifest
        // Skip if we've already digested the tag manifest (tokenizer gets called multiple times)
        if (!filter.contains(tagmanifest)) {
            if (!corrupt && manifest.getFileName().endsWith(tagIdentifier)) {
                tagDigest = DigestUtil.digest(manifest, alg);
                addTokenRequest(manifest, tagDigest);
            }
        }

        return corrupt;
    }

    public String getTagManifestDigest() {
        return tagDigest;
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

    /**
     * Create a connection to the IMS Service for ACE
     *
     * @return {@link TokenRequestBatch}
     */
    private TokenRequestBatch createIMSConnection() {
        IMSService ims;
        // TODO: Use the AceSettings to get the ims host name
        ims = IMSService.connect("ims.umiacs.umd.edu", SSL_PORT, true);
        return ims.createImmediateTokenRequestBatch("SHA-256",
                callback,
                MAX_QUEUE_LEN,
                TIMEOUT);
    }


}
