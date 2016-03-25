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

        Bag bag = r.getBag();
        Node node = r.getNode();

        // TODO: If we're already in a failed state we could skip doing anything
        if (r.getStatus().isFailure()) {
            return;
        }

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
                bag.setStatus(BagStatus.PRESERVED);
            }
        }
    }

}
