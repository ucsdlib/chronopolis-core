package org.chronopolis.rest.listener;

import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagDistribution;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.PreUpdate;
import java.util.Set;

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
        log.trace("In update listener for replication {}", r.getId());
        boolean successToken = false;
        boolean successTag = false;

        Bag bag = r.getBag();
        Node node = r.getNode();

        Long id = bag.getId();
        String username = node.getUsername();
        String receivedTagFixity = r.getReceivedTagFixity();
        String receivedTokenFixity = r.getReceivedTokenFixity();

        // TODO: If we're already in a failed state we could skip doing anything
        if (r.getStatus().isFailure()) {
            return;
        }

        // I'm not sure if this is really the best place to do state updates
        // Since we now have separate endpoints in the controller for token/tag fixity
        // it's easy to separate out the logic for them
        // At the same time, we can use those endpoints to check the state change
        // i.e. have a helper method in the Replication for checkTransferred
        // and have that update the state to transferred if we have both matching fixity values
        // and are still in a non-ace state
        // We could try to use this to validate state changes, however
        // so if we are setting SUCCESS but the received fixities are null, reject
        // I'm not sure if we want to do that, however

        // update state based on the token fixity
            /*
        if (receivedTokenFixity != null) {
            log.debug("Checking token fixity");
            successToken = checkFixity(r, id, username, bag.getTokenDigest(), receivedTokenFixity, ReplicationStatus.FAILURE_TOKEN_STORE);
        }

        // update state based on the tag manifest fixity
        if (receivedTagFixity != null) {
            log.debug("Checking tag fixity");
            successTag = checkFixity(r, id, username, bag.getTagManifestDigest(), receivedTagFixity, ReplicationStatus.FAILURE_TAG_MANIFEST);
        }
        */
        // update state, and BagDistribution if we succeeded
        // TODO: TRANSFERRED/VALIDATED?
        if (r.getStatus() == ReplicationStatus.SUCCESS) {
            log.info("Replication {} successfully transferred", r.getId());
            Set<BagDistribution> distributions = bag.getDistributions();
            for (BagDistribution distribution : distributions) {
                if (distribution.getNode().equals(node)) {
                    distribution.setStatus(BagDistribution.BagDistributionStatus.REPLICATE);
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

    /*
    private boolean checkFixity(Replication r, Long id, String node, String stored, String received, ReplicationStatus failure) {
        if (stored == null || !stored.equals(received)) {
            log.info("Received invalid fixity for bag {} from {}",
                    id,
                    node);
            r.setStatus(failure);
        } else {
            log.info("Matching fixity for {}", r.getId());
            return true;
        }

        return false;
    }
    */

}
