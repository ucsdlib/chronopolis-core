package org.chronopolis.ingest.tokens;

import edu.umiacs.ace.token.TokenStoreWriter;
import org.chronopolis.rest.entities.AceToken;

import java.io.OutputStream;


/**
 * Simple extension of a {@link TokenStoreWriter} for writing our
 * {@link AceToken}s to a file. We make sure that the header information
 * and proofs are written correctly.
 * <p>
 * In addition, we want to keep track of the digest as we write the file
 * so that we can store it for replications to check against.
 * <p>
 * Created by shake on 2/13/15.
 */
public class TokenWriter extends TokenStoreWriter<AceToken> implements AutoCloseable {

    public TokenWriter(OutputStream os) {
        super(os);
    }

    @Override
    public void startToken(final AceToken aceToken) {
        setHeaderInformation(aceToken.getAlgorithm(),
                aceToken.getImsHost(),
                aceToken.getImsService(),
                aceToken.getRound(),
                aceToken.getCreateDate());

        for (String line : aceToken.getProof().split("[\\r\\n]+")) {
            addHashLevel(line);
        }
    }

    public void startProjection(final org.chronopolis.rest.entities.projections.AceToken token) {
        setHeaderInformation(token.getAlgorithm(),
                token.getImsHost(),
                token.getImsService(),
                token.getRound(),
                token.getCreateDate());

        for (String line : token.getProof().split("[\\r\\n]+")) {
            addHashLevel(line);
        }

        String tokenFilename = token.getFilename();
        // startToken(token);
        addIdentifier(tokenFilename.startsWith("/")
                ? tokenFilename
                : "/" + tokenFilename);
    }

}
