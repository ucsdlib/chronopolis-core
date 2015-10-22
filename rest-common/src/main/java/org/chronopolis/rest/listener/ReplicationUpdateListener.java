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
 *
 * Over time if we can, we may want to simply everything idk. This is
 * to get the logic out of the controller.
 *
 * Created by shake on 10/19/15.
 */
public class ReplicationUpdateListener {
    private final Logger log = LoggerFactory.getLogger(ReplicationUpdateListener.class);

    @PreUpdate
    public void updateReplication(Replication r) {
        String digest;
        boolean successToken = false;
        boolean successTag = false;

        Bag bag = r.getBag();
        Node node = r.getNode();
        String receivedTagFixity = r.getReceivedTagFixity();
        String receivedTokenFixity = r.getReceivedTokenFixity();

        // TODO: If we're already in a failed state we could skip doing anything

        // update state based on the token fixity
        if (receivedTokenFixity != null) {
            digest = bag.getTokenDigest();
            if (digest == null || !digest.equals(receivedTokenFixity)) {
                log.info("Received invalid token store fixity for bag {} from {}",
                        bag.getId(),
                        node.getUsername());
                r.setStatus(ReplicationStatus.FAILURE_TOKEN_STORE);
            } else {
                log.info("Matching token store fixity");
                successToken = true;
            }
        }

        // update state based on the tag manifest fixity
        if (receivedTagFixity != null) {
            digest = bag.getTagManifestDigest();
            if (digest == null || !digest.equals(receivedTagFixity)) {
                log.info("Received invalid tag fixity for bag {} from {}",
                        bag.getId(),
                        node.getUsername());
                r.setStatus(ReplicationStatus.FAILURE_TAG_MANIFEST);
            } else {
                log.info("Matching tag fixity");
                successTag = true;
            }
        }

        // update state, and BagDistribution if we succeeded
        if (successTag && successToken && !failureStatus(r.getStatus())) {
            log.info("Replication {} successfully transferred");
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

    private boolean failureStatus(ReplicationStatus status) {
        return status == ReplicationStatus.FAILURE_TOKEN_STORE
            || status == ReplicationStatus.FAILURE_TAG_MANIFEST
            || status == ReplicationStatus.FAILURE;
    }

}
