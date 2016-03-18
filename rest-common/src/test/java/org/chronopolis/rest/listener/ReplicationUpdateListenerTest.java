package org.chronopolis.rest.listener;

import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.Node;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.junit.Assert;
import org.junit.Before;

import static org.chronopolis.rest.models.BagDistribution.BagDistributionStatus.DISTRIBUTE;

/**
 * 4 Tests for our listener:
 * Bad Token Digest
 * Bad Tag Digest
 * Both digests correct
 * Both digests missing
 *
 * Created by shake on 3/4/16.
 */
public class ReplicationUpdateListenerTest {

    private final String TEST_DIGEST = "test-digest";

    Bag b;
    Node n;

    ReplicationUpdateListener listener;

    @Before
    public void setup() {
        n = new Node("test-node", "test-password");

        b = new Bag("test-bag", "test-depositor");
        b.setLocation("test-location");
        b.setTokenLocation("test-location");
        b.setFixityAlgorithm("test-algorithm");
        b.setTokenDigest(TEST_DIGEST);
        b.setTagManifestDigest(TEST_DIGEST);
        b.setStatus(BagStatus.REPLICATING);
        b.setSize(0);
        b.setTotalFiles(0);
        b.addDistribution(n, DISTRIBUTE);

        listener = new ReplicationUpdateListener();
    }

    public Replication createReplication(String tagDigest, String tokenDigest) {
        Replication r = new Replication(n, b);
        r.setNodeUser(n.getUsername());
        r.setProtocol("test-protocol");
        r.setReceivedTagFixity(tagDigest);
        r.setReceivedTokenFixity(tokenDigest);
        return r;
    }

    // @Test
    public void testUpdateBadTokenDigest() throws Exception {
        Replication r =  createReplication(TEST_DIGEST, "bad-digest");

        listener.updateReplication(r);
        Assert.assertEquals(r.getStatus(), ReplicationStatus.FAILURE_TOKEN_STORE);
    }

    // @Test
    public void testUpdateBadTag() throws Exception {
        Replication r = createReplication("bad-digest", TEST_DIGEST);
        listener.updateReplication(r);

        Assert.assertEquals(r.getStatus(), ReplicationStatus.FAILURE_TAG_MANIFEST);
    }

    // @Test
    public void testUpdateCorrect() throws Exception {
        Replication r = createReplication(TEST_DIGEST, TEST_DIGEST);

        listener.updateReplication(r);
        Assert.assertEquals(r.getStatus(), ReplicationStatus.TRANSFERRED);
    }
}