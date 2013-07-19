/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.bagit;

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

/**
 * TODO: Write to output stream
 *
 * @author shake
 */
public class TokenWriterCallback implements RequestBatchCallback {
    // Because I have a ghetto spin lock, we need this to be volatile so that all
    // changes are seen
    private volatile HashMap<String, AceToken> tokenMap;
    private String collectionName;
    private Path manifest;
    
    public TokenWriterCallback(String collectionName) {
        tokenMap = new HashMap<>();
        // denote that it is actually the tokens
        this.collectionName = collectionName+"-tokens";
    }

    public Map<String, AceToken> getTokens() {
        return tokenMap;
    }

    public void writeToFile(Path stage) throws IOException {
        manifest = Paths.get(stage.toString(), collectionName);
        try (OutputStream os = Files.newOutputStream(manifest, CREATE)) {
            AceTokenWriter writer = new AceTokenWriter(os);

            for ( Map.Entry<String, AceToken> token : tokenMap.entrySet()) {
                writer.startToken(token.getValue());
                writer.addIdentifier(token.getKey());
                writer.writeTokenEntry();
            }
            writer.close();
        }
        System.out.println("Finished writing to file");
    }
    
    @Override
    public void tokensReceived(List<TokenRequest> requests, 
                               List<TokenResponse> responses) {
        //Map<String, String> tokenMap = new HashMap<>();
        AceTokenBuilder builder = new AceTokenBuilder();
        
        /*
        for ( TokenRequest tr : requests) {
            tokenMap.put(tr.getName(), tr.getHashValue());
        }
        */

        for ( TokenResponse tr : responses ) {
            
            if ( tr.getStatusCode() == StatusCode.SUCCESS) {
                builder.setDate(tr.getTimestamp().toGregorianCalendar().getTime());
                builder.setDigestAlgorithm(tr.getDigestService());
                builder.setIms("ims.umiacs.umd.edu"); // hard coded cause I'm a punk
                builder.setImsService(tr.getTokenClassName());
                builder.setRound(tr.getRoundId());

                for ( ProofElement p : tr.getProofElements()) {
                    List<String> hashElements = p.getHashes();
                    builder.startProofLevel(hashElements.size()+1);
                    builder.setLevelInheritIndex(p.getIndex());
                    for(String hash : hashElements) {
                        builder.addLevelHash(HashValue.asBytes(hash));
                    }
                } 

                AceToken token = builder.createToken();
                tokenMap.put(tr.getName(), token);
            }
        }
        
    }

    @Override
    public void exceptionThrown(List<TokenRequest> list, Throwable thrwbl) {
        System.out.println("Some other exception!");
        System.out.println(Strings.exceptionAsString(thrwbl));
    }

    @Override
    public void unexpectedException(Throwable thrwbl) {
        System.out.println("Unexpected Error!");
        System.out.println(Strings.exceptionAsString(thrwbl));
    }

    public Path getManifestPath() throws InterruptedException {
        return manifest;
    }
    
}
