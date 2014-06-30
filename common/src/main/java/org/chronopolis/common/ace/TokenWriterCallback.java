package org.chronopolis.common.ace;

import edu.umiacs.ace.exception.StatusCode;
import edu.umiacs.ace.ims.api.RequestBatchCallback;
import edu.umiacs.ace.ims.ws.ProofElement;
import edu.umiacs.ace.ims.ws.TokenRequest;
import edu.umiacs.ace.ims.ws.TokenResponse;
import edu.umiacs.ace.token.AceToken;
import edu.umiacs.ace.token.AceTokenBuilder;
import edu.umiacs.ace.token.AceTokenWriter;
import edu.umiacs.ace.util.HashValue;
import edu.umiacs.util.Strings;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardOpenOption.CREATE;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: I noticed when testing that not all of the manifest files were correct,
 *       so we should check the validity of the AceTokenWriter
 *
 *
 * @author shake
 */
public class TokenWriterCallback implements RequestBatchCallback, Callable<Path> {
    private final Logger log = LoggerFactory.getLogger(TokenWriterCallback.class);
    // I don't think this needs to be volatile anymore since we use a future
    private volatile HashMap<String, AceToken> tokenMap;
    private LinkedBlockingQueue<TokenResponse> tokenCallbacks;
    private String collectionName;
    private String depositor;
    private Path manifest;
    private Path stage = Paths.get("/tmp");

    public TokenWriterCallback(final String collectionName) {
        tokenMap = new HashMap<>();
        this.tokenCallbacks = new LinkedBlockingQueue<>();
        // denote that it is actually the tokens
        this.collectionName = collectionName + "-tokens";
    }

    public TokenWriterCallback(final String collection, final String depositor) {
        tokenMap = new HashMap<>();
        this.tokenCallbacks = new LinkedBlockingQueue<>();
        this.collectionName = collection + "-tokens";
        this.depositor = depositor;
    }

    public Map<String, AceToken> getTokens() {
        return tokenMap;
    }

    public void setStage(final Path stage) {
        this.stage = stage;
    }

    /**
     * Poll the callbacks for token responses and write them to a token store file.
     *
     * @return the path of the written token store
     */
    @Override
    public Path call() {
        Path fullStage = stage;
        if (depositor != null) {
            fullStage = fullStage.resolve(depositor);
        }

        if (!fullStage.toFile().exists()) {
            fullStage.toFile().mkdirs();
        }

        manifest = fullStage.resolve(collectionName);
        try (OutputStream os = Files.newOutputStream(manifest, CREATE)) {
            TokenResponse response;
            AceTokenWriter writer = new AceTokenWriter(os);
            log.info("Polling for token response(s)");
            // 30 seconds for testing, will probably want it to be longer later on
            while ((response = tokenCallbacks.poll(30, TimeUnit.SECONDS)) != null) {
                log.trace("Writing token for response {}", response.getName());
                AceToken token = buildFromResponse(response);
                writer.startToken(token);
                writer.addIdentifier("/" + response.getName());
                writer.writeTokenEntry();
            }
            writer.close();
        } catch (InterruptedException | IOException ex) {
            log.error("Error w/ manifest {} ", ex);
        }
        log.info("Finished writing tokens");

        return manifest;
    }

    private AceToken buildFromResponse(final TokenResponse response) {
        AceTokenBuilder builder = new AceTokenBuilder();
        builder.setDate(response.getTimestamp().toGregorianCalendar().getTime());
        builder.setDigestAlgorithm(response.getDigestService());
        builder.setIms("ims.umiacs.umd.edu"); // hard coded cause I'm a punk
        builder.setImsService(response.getTokenClassName());
        builder.setRound(response.getRoundId());

        for (ProofElement p : response.getProofElements()) {
            List<String> hashElements = p.getHashes();
            builder.startProofLevel(hashElements.size() + 1);
            builder.setLevelInheritIndex(p.getIndex());
            for (String hash : hashElements) {
                builder.addLevelHash(HashValue.asBytes(hash));
            }
        }

        return builder.createToken();
    }

    @Override
    public void tokensReceived(final List<TokenRequest> requests,
                               final List<TokenResponse> responses) {
        log.info("Adding {} token responses", responses.size());
        for (TokenResponse tr : responses) {
            log.trace("Received token response for {}", tr.getName());
            if (tr.getStatusCode() == StatusCode.SUCCESS) {
                tokenCallbacks.add(tr);
            }
        }
    }

    // TODO: Properly handle these exceptions

    @Override
    public void exceptionThrown(final List<TokenRequest> list, final Throwable thrwbl) {
        System.out.println("Some other exception! List size: " + list.size());
        System.out.println(Strings.exceptionAsString(thrwbl));
    }

    @Override
    public void unexpectedException(final Throwable thrwbl) {
        System.out.println("Unexpected Error!");
        System.out.println(Strings.exceptionAsString(thrwbl));
    }

    public Path getManifestPath() throws InterruptedException {
        return manifest;
    }

}
