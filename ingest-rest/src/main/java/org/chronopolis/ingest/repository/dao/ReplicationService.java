package org.chronopolis.ingest.repository.dao;

import com.google.common.collect.ImmutableList;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.ingest.repository.criteria.ReplicationSearchCriteria;
import org.chronopolis.ingest.support.ReplicationCreateResult;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagDistribution;
import org.chronopolis.rest.entities.BagDistributionStatus;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.entities.storage.QStagingStorage;
import org.chronopolis.rest.entities.storage.ReplicationConfig;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.models.create.ReplicationCreate;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;

import static org.chronopolis.ingest.repository.dao.StagingDao.DISCRIMINATOR_BAG;
import static org.chronopolis.ingest.repository.dao.StagingDao.DISCRIMINATOR_TOKEN;


/**
 * Class to help querying for replication objects based on various values.
 * ex: search by node, bag-id, status
 * <p>
 * Created by shake on 5/21/15.
 */
@Component
@Transactional
public class ReplicationService extends SearchService<Replication, Long, ReplicationRepository> {
    private static final String DEFAULT_USER = "chronopolis";
    private final Logger log = LoggerFactory.getLogger(ReplicationService.class);

    private final EntityManager manager;

    private final BagRepository bagRepository;
    private final NodeRepository nodeRepository;

    @Autowired
    public ReplicationService(EntityManager manager,
                              ReplicationRepository replicationRepository,
                              BagRepository bagRepository,
                              NodeRepository nodeRepository) {
        super(replicationRepository);
        this.manager = manager;
        this.bagRepository = bagRepository;
        this.nodeRepository = nodeRepository;
    }


    /**
     * Public method to create a replication based on a bag id and node id
     *
     * @param bagId  the id of the bag to replicate
     * @param nodeId the id of the node to replicate to
     * @return the newly created replication
     * @throws NotFoundException if the bag or node do not exist
     */
    public ReplicationCreateResult create(Long bagId, Long nodeId) {
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
     * @return the newly created replication
     * @throws NotFoundException if the bag or node do not exist
     */
    public ReplicationCreateResult create(ReplicationCreate request) {
        return create(request.getBagId(), request.getNodeId());
    }

    /**
     * Create a replication with a Bag and Node which have already been pulled from the DB.
     * If parameters are met for bag staging storage, continue on by passing along information to
     * a function which will query for token staging storage and continue the replication create process.
     * If active bag storage does not exist or does not have any associated fixity values, return
     * a ReplicationCreateResult with errors outlining the problems.
     *
     * @param bag  The bag to create a replication for
     * @param node The node to send the replication to
     * @return the result of creating the replication
     */
    public ReplicationCreateResult create(final Bag bag, final Node node) {
        Optional<StagingStorage> bagStorage = queryStorage(bag.getId(), DISCRIMINATOR_BAG);
        return bagStorage.filter(staging -> !staging.getFile().getFixities().isEmpty())
                .map(staging -> createReplicationString(staging, true))
                .map(staging -> withBagStorage(bag, node, staging))
                .orElseGet(() -> new ReplicationCreateResult(ImmutableList
                        .of("Problem with BagStorage. Either no active storage or fixities.")));
    }

    /**
     * Complete creation of a Replication with active Bag storage. If active token storage
     * does not exist or does not have an associated fixity values, return a ReplicationCreateResult
     * with errors outlining the problems.
     *
     * @param bag     the bag being replicated
     * @param node    the node receiving the replication
     * @param bagLink the link for replicating the bag
     * @return the result of creating the replication
     */
    private ReplicationCreateResult withBagStorage(Bag bag, Node node, String bagLink) {
        return queryStorage(bag.getId(), DISCRIMINATOR_TOKEN)
                .filter(staging -> !staging.getFile().getFixities().isEmpty())
                .map(staging -> createReplicationString(staging, false))
                .map(tokenLink -> withTokenStorage(bag, node, bagLink, tokenLink))
                .orElseGet(() -> new ReplicationCreateResult(ImmutableList
                        .of("Problem with TokenStorage. Either no active storage or fixities.")));
    }

    /**
     * Complete creation of a Replication, with active Bag and Token storage
     *
     * @param bag       the bag being replicated
     * @param node      the node receiving the replication
     * @param bagLink   the link for replicating the bag
     * @param tokenLink the link for replicating the token store
     * @return the result of creating the replication
     */
    private ReplicationCreateResult withTokenStorage(Bag bag,
                                                     Node node,
                                                     String bagLink,
                                                     String tokenLink) {
        createDist(bag, node);

        ReplicationSearchCriteria criteria = new ReplicationSearchCriteria()
                .withBagId(bag.getId())
                .withNodeUsername(node.getUsername())
                .withStatuses(ReplicationStatus.Companion.active());

        Page<Replication> ongoing = findAll(criteria, new PageRequest(0, 10));
        Replication action = new Replication(ReplicationStatus.PENDING,
                node, bag, bagLink, tokenLink, "rsync", null, null);

        // So... the protocol field needs to be looked at during the next update
        // basically we have a field which is authoritative for both links, even though the
        // bag and token are likely in different staging areas. Either we'll want separate
        // protocol fields, separate replications, or some other way of doing this. Needs thinking.
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
        return new ReplicationCreateResult(action);
    }


    /**
     * Retrieve a StagingStorage entity for a bag
     *
     * @param bagId         the id of the bag
     * @param discriminator the discriminator to join on (either BAG or TOKEN_STORE)
     * @return the StagingStorage entity, wrapped in an Optional in the event none exist
     */
    private Optional<StagingStorage> queryStorage(Long bagId, String discriminator) {
        log.trace("[Bag-{}] Querying storage", bagId);
        QBag b = QBag.bag;
        QStagingStorage storage = QStagingStorage.stagingStorage;

        /*
         * The query we want to mimic
         * SELECT s.id, s.path, s.size, ...
         * FROM staging_storage s
         *   JOIN bag_storage AS bs
         *   ON bs.staging_id = s.id AND bs.bag_id = 12
         * WHERE s.active = 't';
         *
         * Maybe there's a way to do it without the join? All we're doing is getting the staging_storage...
         * SELECT s.id, s.path, s.size, ...
         * FROM staging_storage s
         * WHERE s.active = 't' AND s.id = (SELECT staging_id FROM bag_storage AS b WHERE b.bag_id = ?1);
         *
         * what we end up with seems like a pretty suboptimal query; if needed we can look into it
         * might be easier to execute native sql than fiddle with querydsl in that case
         */
        JPAQueryFactory factory = new JPAQueryFactory(manager);
        JPAQuery<StagingStorage> query = factory.from(b)
                .innerJoin(b.storage, storage)
                .where(storage.active.isTrue().and(storage.file.dtype.eq(discriminator)))
                .select(storage);

        return Optional.ofNullable(query.fetchFirst());
    }

    /**
     * Build a string for replication based off the storage for the object
     * <p>
     * todo: determine who the default user should be
     * todo: might want this to be created by a subclass for the ReplicationConfig (RsyncReplConfig, HttpReplConfig, etc)
     *
     * @param storage The storage to replication from
     * @return The string for the replication
     */
    private String createReplicationString(StagingStorage storage, Boolean trailingSlash) {
        ReplicationConfig config;

        storage.getRegion();
        config = storage.getRegion().getReplicationConfig();

        final String user = config.getUsername() != null ? config.getUsername() : DEFAULT_USER;
        final String server = config.getServer();
        final String root = config.getPath();

        Path path = Paths.get(root, storage.getPath());
        // inline this?
        return buildLink(user, server, path, trailingSlash);
    }

    /**
     * Get or create the BagDistribution for a node
     *
     * @param bag  the bag being distributed
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
            bag.addDistribution(node, BagDistributionStatus.DISTRIBUTE);
            // not sure if this is the best place for this...
            bagRepository.save(bag);
        }
    }

    private String buildLink(String user, String server, Path file, Boolean trailingSlash) {
        return user +
                "@" + server +
                ":" + file.toString() + (trailingSlash ? "/" : "");
    }

}
