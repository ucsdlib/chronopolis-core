package org.chronopolis.rest.listener;

import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagDistribution;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.Node;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.PreUpdate;
import java.util.Set;

import static org.chronopolis.rest.models.BagDistribution.BagDistributionStatus.REPLICATE;

/**
 * Update replications based on their received fixity
 * <p/>
 * Over time if we can, we may want to simply everything idk. This is
 * to get the logic out of the controller.
 * <p/>
 * Created by shake on 10/19/15.
 */
public class ReplicationUpdateListener {
    private final Logger log = LoggerFactory.getLogger(ReplicationUpdateListener.class);

    @PreUpdate
    public void updateReplication(Replication r) {
        boolean successToken = false;
        boolean successTag = false;

        Bag bag = r.getBag();
        Node node = r.getNode();

        Long id = bag.getId();
        String username = node.getUsername();
        String receivedTagFixity = r.getReceivedTagFixity();
        String receivedTokenFixity = r.getReceivedTokenFixity();

        // TODO: If we're already in a failed state we could skip doing anything

        // update state based on the token fixity
        if (receivedTokenFixity != null) {
            successToken = checkFixity(r, id, username, bag.getTokenDigest(), receivedTokenFixity, ReplicationStatus.FAILURE_TOKEN_STORE);
        }

        // update state based on the tag manifest fixity
        if (receivedTagFixity != null) {
            successTag = checkFixity(r, id, username, bag.getTagManifestDigest(), receivedTagFixity, ReplicationStatus.FAILURE_TAG_MANIFEST);
        }

        // update state, and BagDistribution if we succeeded
        if (successTag && successToken && !failureStatus(r.getStatus())) {
            log.info("Replication {} successfully transferred", r.getId());
            r.setStatus(ReplicationStatus.SUCCESS);
            Set<BagDistribution> distributions = bag.getDistributions();
            for (BagDistribution distribution : distributions) {
                if (distribution.getNode().equals(node)) {
                    distribution.setStatus(REPLICATE);
                }
            }

            // TODO: Break out?
            Set<String> nodes = bag.getReplicatingNodes();
            if (nodes.size() >= bag.getRequiredReplications()) {
                log.debug("Setting bag {}::{} as replicated",
                        bag.getDepositor(),
                        bag.getName());
                bag.setStatus(BagStatus.REPLICATED);
            }

        }
    }

    private boolean checkFixity(Replication r, Long id, String node, String stored, String received, ReplicationStatus failure) {
        if (stored == null || !stored.equals(received)) {
            log.info("Received invalid fixity for bag {} from {}",
                    id,
                    node);
            r.setStatus(failure);
        } else {
            log.info("Matching fixity");
            return true;
        }

        return false;
    }

    private boolean failureStatus(ReplicationStatus status) {
        return status == ReplicationStatus.FAILURE_TOKEN_STORE
                || status == ReplicationStatus.FAILURE_TAG_MANIFEST
                || status == ReplicationStatus.FAILURE;
    }

}
