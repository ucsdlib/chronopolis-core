package org.chronopolis.ingest.tokens;

import edu.umiacs.ace.token.TokenStoreWriter;
import org.chronopolis.rest.entities.AceToken;

import java.io.IOException;
import java.io.OutputStream;


/**
 * Simple extension of a {@link TokenStoreWriter} for writing our
 * {@link AceToken}s to a file. We make sure that the header information
 * and proofs are written correctly.
 *
 * In addition, we want to keep track of the digest as we write the file
 * so that we can store it for replications to check against.
 *
 * Created by shake on 2/13/15.
 */
public class TokenWriter extends TokenStoreWriter<AceToken> {
    final String ims;

    public TokenWriter(OutputStream os, String ims) {
        super(os);
        this.ims = ims;
    }

    @Override
    public void startToken(final AceToken aceToken) {
        setHeaderInformation(aceToken.getAlgorithm(),
                ims,
                aceToken.getImsService(),
                aceToken.getRound(),
                aceToken.getCreateDate());

        for (String line : aceToken.getProof().split("[\\r\\n]+")) {
            addHashLevel(line);
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
    }

}
