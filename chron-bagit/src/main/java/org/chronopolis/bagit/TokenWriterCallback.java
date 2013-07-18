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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * TODO: Write to output stream
 *
 * @author shake
 */
public class TokenWriterCallback implements RequestBatchCallback {
    private HashSet<AceToken> tokenSet;

    public TokenWriterCallback() {
        tokenSet = new HashSet<>();
    }
    
    @Override
    public void tokensReceived(List<TokenRequest> requests, 
                               List<TokenResponse> responses) {
        System.out.println("Recieved callback!");
        Map<String, String> tokenMap = new HashMap<>();
        AceTokenBuilder builder = new AceTokenBuilder();
        
        for ( TokenRequest tr : requests) {
            tokenMap.put(tr.getName(), tr.getHashValue());
        }

        for ( TokenResponse tr : responses ) {
            
            if ( tr.getStatusCode() == StatusCode.SUCCESS) {
                // tokenSet.add(tr);
                builder.setDate(tr.getTimestamp().toGregorianCalendar().getTime());
                System.out.println(tr.getDigestService() + " :: " + tr.getDigestProvider());
                builder.setDigestAlgorithm(tr.getDigestService());
                builder.setIms("ims.umiacs.umd.edu"); // hard coded cause I'm a punk
                builder.setImsService(tr.getTokenClassName());
                // builder.setLevelInheritIndex(0);
                builder.setRound(tr.getRoundId());

                // Something here is wrong... 
                for ( ProofElement p : tr.getProofElements()) {
                    List<String> hashElements = p.getHashes();
                    System.out.println(p.getIndex());
                    System.out.println(hashElements.size());
                    builder.startProofLevel(hashElements.size()+1);
                    builder.setLevelInheritIndex(p.getIndex());
                    for(String s : hashElements) {
                        builder.addLevelHash(HashValue.asBytes(s));
                    }
                } 

                AceToken token = builder.createToken();
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
    
}
