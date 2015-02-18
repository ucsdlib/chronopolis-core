package org.chronopolis.ingest;

import com.google.common.hash.HashingOutputStream;
import edu.umiacs.ace.token.TokenStoreWriter;
import org.chronopolis.rest.models.AceToken;

import javax.annotation.Nullable;
import java.io.IOException;


/**
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


    /*
    @Override
    public void writeTokenEntry() throws IOException {
        Check.notNegative("IdentifierList Size", Integer.valueOf(this.identifierList.size()));
        Check.notNegative("IdentifierList Size", Integer.valueOf(this.levelList.size()));
        Check.notNegative("Round", Long.valueOf(this.round));
        String header = this.digestAlgorithm + " " + this.ims + " " + this.imsService + " " + this.round + " " + this.formattedDate + " ";
        StringBuilder entryBody = new StringBuilder();
        Iterator block = this.identifierList.iterator();

        String level;
        while(block.hasNext()) {
            level = (String)block.next();
            entryBody.append(level);
            entryBody.append("\n");
        }

        entryBody.append("\n");
        block = this.levelList.iterator();

        while(block.hasNext()) {
            level = (String)block.next();
            entryBody.append(level);
            entryBody.append("\n");
        }

        entryBody.append("\n");
        byte[] block1 = entryBody.toString().getBytes("UTF-8");
        header = header + block1.length + "\n";
        this.tokenStore.write(header.getBytes("UTF-8"));
        this.tokenStore.write(block1);
        this.clear();
    }
    */

}
