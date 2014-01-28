/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
    private final String fixityAlgorithm;
    private final Set<Path> manifests;
    private TokenWriterCallback callback = null;
    private TokenRequestBatch batch = null;


    public BagTokenizer(Path bag, String fixityAlgorithm) {
        this.bag = bag;
        this.fixityAlgorithm = fixityAlgorithm;
        this.manifests = new HashSet<>();
        this.callback = new TokenWriterCallback(this.bag.getFileName().toString());
        addManifests();
    }

    private void addManifests() {
        Path tagManifest = bag.resolve("tagmanifest-"+fixityAlgorithm+".txt");
        Path manifest = bag.resolve("manifest-"+fixityAlgorithm+".txt");

        if ( !tagManifest.toFile().exists() ) {
            throw new RuntimeException("TagManifest does not exist!");
        }
        if ( !manifest.toFile().exists() ) {
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
     */
    public Path getAceManifestWithValidation() throws InterruptedException, ExecutionException {
        // Final digest list
        HashMap<Path, String> digests = new HashMap<>();
        Set<Path> badFiles = new HashSet<>();
        String line;

        // Validate our give manifests
        for ( Path manifest : manifests ) {
            try {
                BufferedReader br = Files.newBufferedReader(manifest, Charset.forName("UTF-8"));
                while ( (line = br.readLine()) != null ) {
                    String [] split = line.split("\\s+", 1);
                    String digest = split[0];
                    Path path = Paths.get(bag.toString(), split[1]);
                    String calculatedDigest = DigestUtil.digest(path, fixityAlgorithm);
                    if ( digest.equals(calculatedDigest) ) {
                        digests.put(path, digest);
                    } else {
                        badFiles.add(path);
                    }
                }
            } catch (IOException e) {
                log.error("Error reading file " + manifest.toString());
            }

            // This only runs against 2 files, don't really need to try and be clever
            if (manifest.toString().contains("tagmanifest")){
                String tagDigest = DigestUtil.digest(manifest, fixityAlgorithm);
                digests.put(manifest, tagDigest);
            }
        }

        // F it
        if ( !badFiles.isEmpty() ) {
            StringBuilder files = new StringBuilder();
            for ( Path p : badFiles ) {
                files.append(p.toString());
                files.append("\n");
            }
            log.error("Error validating collection, ( " + badFiles.size() + " ) bad files found: " + files.toString());
            return null;
        }

        // Token creation
        createIMSConnection();
        callback.setStage(Paths.get("/tmp")); // TODO: Token stage
        Future<Path> manifest = manifestService.submit(callback);
        for ( Map.Entry<Path, String> entry : digests.entrySet()) {
            TokenRequest req = new TokenRequest();
            Path full = entry.getKey();
            Path relative = full.subpath(bag.getNameCount(), full.getNameCount());

            req.setName(relative.toString());
            req.setHashValue(entry.getValue());
            batch.add(req);
        }

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
    public Path getAceManifestWithoutValidation(Path stage) throws InterruptedException,
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

        return manifestPath.get();
    }

    private void createIMSConnection() {
        IMSService ims;
        // TODO: Unhardcode
        ims = IMSService.connect("ims.umiacs.umd.edu", 443, true);
        batch = ims.createImmediateTokenRequestBatch("SHA-256",
                callback,
                1000,
                5000);
    } 
}
