package org.chronopolis.ingest;

import com.google.common.hash.HashingOutputStream;
import edu.umiacs.ace.token.TokenStoreWriter;
import org.chronopolis.rest.models.AceToken;

import javax.annotation.Nullable;
import java.io.IOException;


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
    final HashingOutputStream os;

    @Nullable
    private String digest;

    public TokenWriter(HashingOutputStream os, String ims) {
        super(os);
        this.os = os;
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

    public String getTokenDigest() {
        return digest;
    }

    @Override
    public void close() throws IOException {
        super.close();

        this.digest = os.hash().toString();
    }

}
