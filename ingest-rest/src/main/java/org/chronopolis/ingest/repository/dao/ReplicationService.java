package org.chronopolis.ingest.repository.dao;

import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.ingest.repository.criteria.ReplicationSearchCriteria;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagDistribution;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.ReplicationRequest;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import static org.chronopolis.rest.entities.BagDistribution.BagDistributionStatus.DISTRIBUTE;

/**
 * Class to help querying for replication objects based on various values.
 * ex: search by node, bag-id, status
 *
 * Created by shake on 5/21/15.
 */
@Component
@Transactional
public class ReplicationService extends SearchService<Replication, Long, ReplicationRepository>{
    private final Logger log = LoggerFactory.getLogger(ReplicationService.class);

    private final ReplicationRepository replicationRepository;
    private final BagRepository bagRepository;
    private final NodeRepository nodeRepository;

    @Autowired
    public ReplicationService(ReplicationRepository replicationRepository,
                              BagRepository bagRepository,
                              NodeRepository nodeRepository) {
        super(replicationRepository);
        this.replicationRepository = replicationRepository;
        this.bagRepository = bagRepository;
        this.nodeRepository = nodeRepository;
    }


    /**
     * Public method to create a replication based on a bag id and node id
     *
     * @param bagId the id of the bag to replicate
     * @param nodeId the id of the node to replicate to
     * @throws NotFoundException if the bag or node do not exist
     * @return the newly created replication
     */
    public Replication create(Long bagId, Long nodeId, IngestSettings settings) {
        log.debug("Processing request for Bag {}", bagId);

        // Get our db resources
        Node node = nodeRepository.findOne(nodeId);
        Bag bag = bagRepository.findOne(bagId);

        if (bag == null) {
            throw new NotFoundException("Bag " + bagId);
        } else if (node == null) {
            throw new NotFoundException("Node " + nodeId);
        }

        return create(bag, node, settings);
    }

    /**
     * Create a new replication for the Node (user) based on the Bag Id
     * If a replication already exists (and is not terminated), return it instead of creating a new one
     *
     * @param request The request to create a replication for
     * @param settings The settings for basic information
     * @throws NotFoundException if the bag or node do not exist
     * @return the newly created replication
     */
    public Replication create(ReplicationRequest request, IngestSettings settings) {
       return create(request.getBagId(), request.getNodeId(), settings);
    }

    /**
     * Create a replication with a Bag and Node which have already been
     * pulled from the DB
     *
     * @param bag The bag to create a replication for
     * @param node The node to send the replication to
     * @param settings The settings for basic information
     * @return the newly created replication
     */
    public Replication create(final Bag bag, final Node node, IngestSettings settings) {

        // create a dist object if it's missing
        // todo: move this out of the replication create
        createDist(bag, node);

        // vars to help create replication stuff
        // todo: from replication_config
        final String user = settings.getReplicationUser();
        final String server = settings.getStorageServer();
        final String bagStage = settings.getRsyncBags();
        final String tokenStage = settings.getRsyncTokens();

        Path tokenPath = Paths.get(tokenStage, bag.getTokenStorage().getPath());
        String tokenLink =  buildLink(user, server, tokenPath);

        Path bagPath = Paths.get(bagStage, bag.getBagStorage().getPath());
        String bagLink = buildLink(user, server, bagPath);

        // TODO: Allow searching for multiple status
        ReplicationSearchCriteria criteria = new ReplicationSearchCriteria()
                .withBagId(bag.getId())
                .withNodeUsername(node.getUsername());

        Page<Replication> ongoing = findAll(criteria, new PageRequest(0, 10));
        Replication action = new Replication(node, bag, bagLink, tokenLink);
        action.setProtocol("rsync"); // TODO: Magic val... once we update our storage model this can be updated

        // iterate through our ongoing replications and search for a non terminal replication
        // TODO: Partial index this instead:
        //       create unique index "one_repl" on replications(node_id) where status == ''...
        if (ongoing.getTotalElements() != 0) {
            for (Replication replication : ongoing.getContent()) {
                ReplicationStatus status = replication.getStatus();
                if (status.isOngoing()) {
                    log.info("Found ongoing replication for {} to {}, ignoring create request",
                            bag.getName(), node.getUsername());
                    action = replication;
                }
            }
        } else {
            log.info("Created new replication request for {} to {}",
                    bag.getName(), node.getUsername());
        }

        save(action);
        return action;
    }

    /**
     * Get or create the BagDistribution for a node
     *
     * @param bag the bag being distributed
     * @param node the node being distributed to
     */
    private void createDist(Bag bag, Node node) {
        BagDistribution bagDistribution = null;
        // todo: see if there's a way to query this instead of iterate
        Set<BagDistribution> distributions = bag.getDistributions();
        for (BagDistribution distribution : distributions) {
            if (distribution.getNode().equals(node)) {
                bagDistribution = distribution;
            }
        }

        if (bagDistribution == null) {
            bag.addDistribution(node, DISTRIBUTE);
            // not sure if this is the best place for this...
            bagRepository.save(bag);
        }
    }

    private String buildLink(String user, String server, Path file) {
        return user +
                "@" + server +
                ":" + file.toString();
    }

}
