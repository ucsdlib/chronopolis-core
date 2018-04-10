package org.chronopolis.tokenize.batch;

import edu.umiacs.ace.ims.ws.TokenRequest;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.TokenWorkSupervisor;

import java.util.Set;

/**
 * An object used by Chronopolis for Tokenizing {@link ManifestEntry} objects in a batch process.
 *
 * Initially this was just supposed to be a marker interface for batch operations, but it has a few
 * methods now which all request batches should share. Processing and updating is implementation
 * dependent, and {@link TokenWorkSupervisor}s might not necessarily push requests to the process method.
 *
 * @author shake
 */
public interface TokenRequestBatch {

    /**
     * Process a {@link Set} of {@link ManifestEntry} objects to process and send
     * {@link TokenRequest}s to the ACE IMS for creation of ACE Tokens
     *
     * @param entries the ManifestEntries to create ACE Tokens for
     */
    void process(Set<ManifestEntry> entries);

    /**
     * Create an ACE {@link TokenRequest} from a ManifestEntry
     *
     * @param entry The ManifestEntry to create a TokenRequest for
     * @return the TokenRequest
     */
    default TokenRequest createRequest(ManifestEntry entry) {
        TokenRequest request = new TokenRequest();
        request.setHashValue(entry.getCalculatedDigest());
        request.setName(entry.tokenName());
        return request;
    }
}
