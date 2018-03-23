package org.chronopolis.rest.listener;

import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.models.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.PreUpdate;
import java.util.Set;

/**
 * Look to see if a bag has been completely replicated and update if necessary
 *
 * Created by shake on 10/19/15.
 */
public class BagUpdateListener {
    private final Logger log = LoggerFactory.getLogger(BagUpdateListener.class);

    @PreUpdate
    public void updateBagStatus(Bag bag) {
        // Then check to see if the bag has been fully replicated
        Set<String> nodes = bag.getReplicatingNodes();
        Depositor depositor = bag.getDepositor();
        if (nodes.size() >= depositor.getNodeDistributions().size()) {
            log.debug("Setting bag {}::{} as replicated",
                    bag.getDepositor(),
                    bag.getName());
            bag.setStatus(BagStatus.PRESERVED);
        }
    }

}
