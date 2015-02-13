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
import java.util.HashSet;
import java.util.Set;

/**
 * Created by shake on 2/4/15.
 */
public class Tokenizer {
    private static final int SSL_PORT = 443;
    private static final int MAX_QUEUE_LEN = 1000;
    private static final int TIMEOUT = 5000;

    private final Logger log = LoggerFactory.getLogger(Tokenizer.class);
    private final Path bag;
    private final Digest fixityAlgorithm;
    private final Set<Path> manifests;

    private final RequestBatchCallback callback;
    private TokenRequestBatch batch;

    public Tokenizer(final Path bag,
                     final String fixityAlgorithm,
                     final RequestBatchCallback callback) {
        this.bag = bag;
        this.fixityAlgorithm = Digest.fromString(fixityAlgorithm);
        this.manifests = new HashSet<>();
        this.callback = callback;
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

    public void tokenize(Set<Path> filter) throws IOException, InterruptedException {
        String line;
        String alg = fixityAlgorithm.getName();
        batch = createIMSConnection();

        for (Path manifest : manifests) {
            BufferedReader br = Files.newBufferedReader(manifest, Charset.defaultCharset());
            boolean corrupt = false;
            while ((line = br.readLine()) != null) {
                String []split = line.split("\\s+", 2);
                if (split.length != 2) {
                    log.error("Error found in manifest: {}", split);
                    continue;
                }

                String digest = split[0];
                Path rel = Paths.get(split[1]);
                if (filter.contains(rel)) {
                    continue;
                }

                Path path = Paths.get(bag.toString(), split[1]);
                String calculatedDigest = DigestUtil.digest(path, alg);

                if (digest.equals(calculatedDigest)) {
                    addTokenRequest(path, digest);
                } else {
                    corrupt = true;
                }
            }

            if (!corrupt && manifest.getFileName().endsWith("tagmanifest-sha256.txt")) {
                String manifestDigest = DigestUtil.digest(manifest, alg);
                addTokenRequest(manifest, manifestDigest);
            }
        }

        batch.close();
    }

    private void addTokenRequest(Path path, String digest) throws InterruptedException {
        Path rel = path.subpath(bag.getNameCount(), path.getNameCount());
        TokenRequest req = new TokenRequest();
        req.setHashValue(digest);
        req.setName(rel.toString());
        batch.add(req);
    }

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
