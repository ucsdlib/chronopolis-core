package org.chronopolis.ingest.repository.dao;

import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.ingest.repository.criteria.ReplicationSearchCriteria;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagDistribution;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.entities.storage.ReplicationConfig;
import org.chronopolis.rest.entities.storage.StagingStorage;
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
    private static final String DEFAULT_USER = "chronopolis";
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
    public Replication create(Long bagId, Long nodeId) {
        log.debug("Processing request for Bag {}", bagId);

        // Get our db resources
        Node node = nodeRepository.findOne(nodeId);
        Bag bag = bagRepository.findOne(bagId);

        if (bag == null) {
            throw new NotFoundException("Bag " + bagId);
        } else if (node == null) {
            throw new NotFoundException("Node " + nodeId);
        }

        return create(bag, node);
    }

    /**
     * Create a new replication for the Node (user) based on the Bag Id
     * If a replication already exists (and is not terminated), return it instead of creating a new one
     *
     * @param request The request to create a replication for
     * @throws NotFoundException if the bag or node do not exist
     * @return the newly created replication
     */
    public Replication create(ReplicationRequest request) {
       return create(request.getBagId(), request.getNodeId());
    }

    /**
     * Create a replication with a Bag and Node which have already been
     * pulled from the DB
     *
     * @param bag The bag to create a replication for
     * @param node The node to send the replication to
     * @return the newly created replication
     */
    public Replication create(final Bag bag, final Node node) {
        // create a dist object if it's missing
        // todo: move this out of the replication create
        createDist(bag, node);

        String tokenLink = createReplicationString(bag.getTokenStorage());
        String bagLink = createReplicationString(bag.getBagStorage());

        // todo: query for ongoing replications
        ReplicationSearchCriteria criteria = new ReplicationSearchCriteria()
                .withBagId(bag.getId())
                .withNodeUsername(node.getUsername());

        Page<Replication> ongoing = findAll(criteria, new PageRequest(0, 10));
        Replication action = new Replication(node, bag, bagLink, tokenLink);

        // TODO: Magic val... we should be able to get this from the replication config soon
        action.setProtocol("rsync");

        // iterate through our ongoing replications and search for a non terminal replication
        // Partial index this instead?
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
     * Build a string for replication based off the storage for the object
     *
     * todo: determine who the default user should be
     *
     * @param storage The storage to replication from
     * @return The string for the replication
     */
    private String createReplicationString(StagingStorage storage) {
        ReplicationConfig config;

        if (storage.getRegion() != null && storage.getRegion().getReplicationConfig() != null) {
            config = storage.getRegion()
                    .getReplicationConfig();
        } else {
            // Probably want something different from a RuntimeException, but for now this should suffice
            throw new RuntimeException("Unable to create replication for storage object " + storage.getId());
        }

        final String user = config.getUsername() != null ? config.getUsername() : DEFAULT_USER;
        final String server = config.getServer();
        final String root = config.getPath();

        Path path = Paths.get(root, storage.getPath());
        // inline this?
        return buildLink(user, server, path);
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
