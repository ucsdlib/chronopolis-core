package org.chronopolis.ingest;

import edu.umiacs.ace.token.TokenStoreWriter;
import org.chronopolis.rest.models.AceToken;

import java.io.OutputStream;

/**
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
}
