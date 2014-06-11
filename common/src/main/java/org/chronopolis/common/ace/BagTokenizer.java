package org.chronopolis.common.ace;

import edu.umiacs.ace.ims.api.IMSService;
import edu.umiacs.ace.ims.api.TokenRequestBatch;
import edu.umiacs.ace.ims.ws.TokenRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.chronopolis.common.digest.Digest;
import org.chronopolis.common.digest.DigestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Create Ace Tokens for a bag
 * TODO: Consolidation
 *
 * @author shake
 */
public class BagTokenizer {
    private final Logger log = LoggerFactory.getLogger(BagTokenizer.class);
    private final ExecutorService manifestService = Executors.newCachedThreadPool();
    private final Path bag;
    private final Path tokenStage;
    private final Digest fixityAlgorithm;
    private final Set<Path> manifests;

    private TokenWriterCallback callback = null;
    private TokenRequestBatch batch = null;
    private String tagManifestDigest;

    private static final int SSL_PORT = 443;
    private static final int MAX_QUEUE_LEN = 1000;
    private static final int TIMEOUT = 5000;

    public BagTokenizer(final Path bag, final Path tokenStage, final String fixityAlgorithm) {
        this.bag = bag;
        this.tokenStage = tokenStage;
        this.fixityAlgorithm = Digest.fromString(fixityAlgorithm);
        this.manifests = new HashSet<>();
        this.callback = new TokenWriterCallback(this.bag.getFileName().toString());
        addManifests();
    }

    private void addManifests() {
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

        manifests.add(tagManifest);
        manifests.add(manifest);
    }

    /**
     * Create an ACE Token Manifest, validating that files are correct as we go
     * along
     *
     * @return The path to the token manifest
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public Path getAceManifestWithValidation() throws InterruptedException, ExecutionException {
        // Final digest list
        HashMap<Path, String> digests = new HashMap<>();
        Set<Path> badFiles = new HashSet<>();
        String line;

        // Validate our given manifests
        for (Path manifest : manifests) {
            try {
                BufferedReader br = Files.newBufferedReader(manifest, Charset.forName("UTF-8"));
                while ((line = br.readLine()) != null) {
                    log.trace("Processing {}", line);
                    String [] split = line.split("\\s+", 2);
                    String digest = split[0];
                    Path path = Paths.get(bag.toString(), split[1]);
                    String calculatedDigest = DigestUtil.digest(path, fixityAlgorithm.getName());
                    if (digest.equals(calculatedDigest)) {
                        digests.put(path, digest);
                    } else {
                        Object[] stf = new Object[]{
                                path.toString(), calculatedDigest, digest
                        };
                        log.error("Bad manifest for {}, found {} but expected {}",
                                stf);
                        badFiles.add(path);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading file " + manifest.toString());
                throw new RuntimeException("Could not create ace manifest");
            }

            // This only runs against 2 files, don't really need to try and be clever
            if (manifest.toString().contains("tagmanifest")) {
                tagManifestDigest = DigestUtil.digest(manifest, fixityAlgorithm.getName());
                digests.put(manifest, tagManifestDigest);
            }
        }

        // F it
        if (!badFiles.isEmpty()) {
            StringBuilder files = new StringBuilder();
            for (Path p : badFiles) {
                files.append(p.toString());
                files.append("\n");
            }
            log.error("Error validating collection, ({}) bad files found:\n{}",
                    badFiles.size(), files.toString());
            return null;
        }

        log.info("Creating tokens");
        // Token creation
        createIMSConnection();
        callback.setStage(tokenStage);
        Future<Path> manifest = manifestService.submit(callback);
        for (Map.Entry<Path, String> entry : digests.entrySet()) {
            TokenRequest req = new TokenRequest();
            Path full = entry.getKey();
            Path relative = full.subpath(bag.getNameCount(), full.getNameCount());
            log.trace("Adding {} to batch", relative.toString());

            req.setName(relative.toString());
            req.setHashValue(entry.getValue());
            batch.add(req);
        }
        log.info("Closing token request");
        batch.close();

        return manifest.get();
    }


    /**
     * Create an ACE Token Manifest from the manifest-alg.txt and tagmanifest-alg.txt
     * files.
     *
     * @param stage The token stage
     * @return The path to the token manifest
     * @throws InterruptedException
     * @throws IOException
     * @throws ExecutionException
     */
    public Path getAceManifestWithoutValidation(final Path stage) throws InterruptedException,
            IOException,
            ExecutionException {
        // temp while I figure out what to do
        HashMap<Path, String> validDigests = new HashMap<>();
        /*
        if (!validManifest.isDone()) {
            throw new RuntimeException("Not finished validating manifest for bag");
        }
        */

        if (stage == null) {
            throw new RuntimeException("Stage cannot be null");
        }

        createIMSConnection();
        callback.setStage(stage);
        Future<Path> manifestPath = manifestService.submit(callback);

        log.info("Have {} entries", validDigests.entrySet().size());
        for (Map.Entry<Path, String> entry : validDigests.entrySet()) {
            TokenRequest req = new TokenRequest();
            // We want the relative path for ACE so let's get it
            Path full = entry.getKey();
            Path relative = full.subpath(bag.getNameCount(), full.getNameCount());

            req.setName(relative.toString());
            req.setHashValue(entry.getValue());
            batch.add(req);
        }
        batch.close();

        return manifestPath.get();
    }

    private void createIMSConnection() {
        IMSService ims;
        // TODO: Unhardcode ims server
        ims = IMSService.connect("ims.umiacs.umd.edu", SSL_PORT, true);
        batch = ims.createImmediateTokenRequestBatch("SHA-256",
                callback,
                MAX_QUEUE_LEN,
                TIMEOUT);
    }

    public String getTagManifestDigest() {
        return tagManifestDigest;
    }

}
