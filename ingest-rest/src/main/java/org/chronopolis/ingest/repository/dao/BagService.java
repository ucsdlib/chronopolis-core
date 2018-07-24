package org.chronopolis.ingest.repository.dao;

import com.google.common.collect.ImmutableList;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.support.BagCreateResult;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.DepositorNode;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QDepositor;
import org.chronopolis.rest.entities.storage.QStorageRegion;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.kot.models.create.BagCreate;
import org.chronopolis.rest.models.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

import static org.chronopolis.rest.entities.BagDistribution.BagDistributionStatus.DISTRIBUTE;

/**
 * Service to build queries for bags from the search criteria
 * <p>
 * Created by shake on 5/20/15.
 */
@Transactional
public class BagService extends SearchService<Bag, Long, BagRepository> {

    private final Logger log = LoggerFactory.getLogger(BagService.class);

    private final EntityManager entityManager;

    public BagService(BagRepository bagRepository, EntityManager entityManager) {
        super(bagRepository);
        this.entityManager = entityManager;
    }

    /**
     * Retrieve a List of DEPOSITED Bags which have the same amount of ACE Tokens registered
     * as total files
     *
     * The query is equivalent to:
     * SELECT * FROM bag b
     *   WHERE status = 'DEPOSITED' AND
     *         total_files = (SELECT count(id) FROM ace_token WHERE bag_id = b.id);
     *
     *
     * @return the List of Bags matching the query
     */
    public List<Bag> getBagsWithAllTokens() {
        JPAQueryFactory factory = new JPAQueryFactory(entityManager);
        QBag bag = QBag.bag;
        QAceToken token = QAceToken.aceToken;
        return factory.selectFrom(bag)
                .where(bag.status.eq(BagStatus.DEPOSITED),
                        bag.totalFiles.eq(
                                JPAExpressions.select(token.id.count())
                                        .from(token)
                                        .where(token.bag.id.eq(bag.id))))
                .fetch();
    }

    /**
     * Process an IngestRequest for creating a Bag and return the result of the operation
     *
     * @param creator the user who initiated the request
     * @param request the request
     * @return the result of creating the Bag
     */
    public BagCreateResult processRequest(String creator,
                                          BagCreate request) {
        JPAQueryFactory queryFactory = new JPAQueryFactory(entityManager);
        return fetchStorageRegion(creator, request, queryFactory);
    }

    /**
     * Fetch a StorageRegion for creating a Bag
     * <p>
     * If a StorageRegion is not found with the given id, return a BagCreateResult with a
     * BAD_REQUEST status
     *
     * @param creator the user who initiated the request
     * @param request the IngestRequest containing the Bag's information
     * @param factory the QueryFactory for accessing the DB
     * @return the result of creating the Bag
     */
    private BagCreateResult fetchStorageRegion(String creator,
                                               BagCreate request,
                                               JPAQueryFactory factory) {
        Long id = request.getStorageRegion();
        ImmutableList<String> error = ImmutableList.of("StorageRegion does not exist: " + id);
        return Optional.ofNullable(
                factory.selectFrom(QStorageRegion.storageRegion)
                        .where(QStorageRegion.storageRegion.id.eq(id))
                        .fetchOne())
                .map(region -> fetchDepositor(creator, request, region, factory))
                .orElse(new BagCreateResult(error, BagCreateResult.Status.BAD_REQUEST));
    }

    /**
     * Fetch a Depositor for creating a Bag
     * <p>
     * If a Depositor is not found with the given namespace, return a BagCreateResult with a
     * BAD_REQUEST status
     *
     * @param creator the user who initiated the request
     * @param request the IngestRequest containing the Bag's information
     * @param region  the StorageRegion the staged content resides in
     * @param factory the QueryFactory for accessing the DB
     * @return the result of creating the Bag
     */
    private BagCreateResult fetchDepositor(String creator,
                                           BagCreate request,
                                           StorageRegion region,
                                           JPAQueryFactory factory) {
        String namespace = request.getDepositor();
        ImmutableList<String> error = ImmutableList.of("Depositor does not exist: " + namespace);
        return Optional.ofNullable(
                factory.selectFrom(QDepositor.depositor)
                        .where(QDepositor.depositor.namespace.eq(namespace))
                        .fetchOne())
                .map(depositor -> create(creator, request, region, depositor, factory))
                .orElse(new BagCreateResult(error, BagCreateResult.Status.BAD_REQUEST));
    }

    /**
     * Create a Bag with all correct parameters
     * <p>
     * If it is found that a Bag already exists with a given name in Chronopolis, a BagCreateResult
     * with a Conflict status will be returned.
     *
     * @param creator   the user who initiated the request
     * @param request   the IngestRequest containing the Bag's information
     * @param region    the StorageRegion the staged content resides in
     * @param depositor the Depositor who is considered the data's owner
     * @param factory   the QueryFactory for accessing the DB
     * @return the result of creating the Bag
     */
    private BagCreateResult create(String creator,
                                   BagCreate request,
                                   StorageRegion region,
                                   Depositor depositor,
                                   JPAQueryFactory factory) {
        BagCreateResult result;
        ImmutableList<String> errors = ImmutableList.of("Bag already exists: " + request.getName());

        // As of 1.6.06 bags have a unique name constraint, so we can search only on the name
        Bag existing = factory.selectFrom(QBag.bag)
                .where(QBag.bag.name.eq(request.getName()))
                .fetchOne();

        if (existing == null) {
            Bag bag = new Bag(request.getName(), depositor)
                    .setCreator(creator)
                    .setSize(request.getSize())
                    .setTotalFiles(request.getTotalFiles());

            StagingStorage storage = new StagingStorage();
            storage.setRegion(region);
            storage.setActive(true);
            storage.setSize(request.getSize());
            storage.setTotalFiles(request.getTotalFiles());
            storage.setPath(request.getLocation());
            bag.setBagStorage(storage);
            createDistributions(bag, depositor);

            save(bag);
            result = new BagCreateResult(bag);
        } else {
            result = new BagCreateResult(errors, BagCreateResult.Status.CONFLICT);
        }

        return result;
    }

    /**
     * Create BagDistributions based on the DepositorNode distributions from a Depositor
     *
     * @param bag       the bag to create distributions for
     * @param depositor the depositor of the bag
     */
    private void createDistributions(Bag bag, Depositor depositor) {
        for (DepositorNode node : depositor.getNodeDistributions()) {
            log.debug("Creating requested dist record for {}", node.getNode().username);
            bag.addDistribution(node.getNode(), DISTRIBUTE);
        }
    }

}
