package org.chronopolis.ingest.repository.dao;

import com.google.common.collect.ImmutableList;
import com.querydsl.core.group.GroupBy;
import com.querydsl.jpa.impl.JPAQuery;
import org.chronopolis.ingest.models.filter.BagFilter;
import org.chronopolis.ingest.support.BagCreateResult;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagDistributionStatus;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagDistribution;
import org.chronopolis.rest.entities.QDataFile;
import org.chronopolis.rest.entities.QNode;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.depositor.QDepositor;
import org.chronopolis.rest.entities.projections.CompleteBag;
import org.chronopolis.rest.entities.projections.PartialBag;
import org.chronopolis.rest.entities.storage.QStagingStorage;
import org.chronopolis.rest.models.create.BagCreate;
import org.chronopolis.rest.models.enums.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.repository.support.PageableExecutionUtils;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.chronopolis.rest.entities.depositor.QDepositor.depositor;

public class BagDao extends PagedDao {

    private final Logger log = LoggerFactory.getLogger(BagDao.class);

    public BagDao(EntityManager em) {
        super(em);
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
        return fetchDepositor(creator, request);
    }

    /**
     * Fetch a Depositor for creating a Bag
     * <p>
     * If a Depositor is not found with the given namespace, return a BagCreateResult with a
     * BAD_REQUEST status
     *
     * @param creator the user who initiated the request
     * @param request the IngestRequest containing the Bag's information
     * @return the result of creating the Bag
     */
    private BagCreateResult fetchDepositor(String creator,
                                           BagCreate request) {
        String namespace = request.getDepositor();
        ImmutableList<String> error = ImmutableList.of("Depositor does not exist: " + namespace);
        return Optional.ofNullable(
                getJPAQueryFactory().selectFrom(depositor)
                        .where(depositor.namespace.eq(namespace))
                        .fetchOne())
                .map(depositor -> create(creator, request, depositor))
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
     * @param depositor the Depositor who is considered the data's owner
     * @return the result of creating the Bag
     */
    private BagCreateResult create(String creator,
                                   BagCreate request,
                                   Depositor depositor) {
        BagCreateResult result;
        ImmutableList<String> errors = ImmutableList.of("Bag already exists: " + request.getName());

        // As of 1.6.06 bags have a unique name constraint, so we can search only on the name
        Bag existing = getJPAQueryFactory().selectFrom(QBag.bag)
                .where(QBag.bag.name.eq(request.getName()))
                .fetchOne();

        if (existing == null) {
            Bag bag = new Bag(request.getName(),
                    creator,
                    depositor,
                    request.getSize(),
                    request.getTotalFiles(),
                    BagStatus.DEPOSITED);

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
        for (Node node : depositor.getNodeDistributions()) {
            log.debug("Creating requested dist record for {}", node.getUsername());
            bag.addDistribution(node, BagDistributionStatus.DISTRIBUTE);
        }
    }

    /**
     * Retrieve a List of {@link PartialBag} views which will not further query the database for
     * related tables
     *
     * @param filter the {@link BagFilter} containing the query parameters
     * @return the result of the database query
     */
    public List<PartialBag> partialViews(BagFilter filter) {
        QBag bag = QBag.bag;
        return partialQuery(filter)
                .transform(GroupBy.groupBy(bag.id).list(partialProjection()));
    }

    /**
     * Retrieve a {@link Page} of {@link PartialBag} views
     *
     * @param filter the {@link BagFilter} containing the query parameters
     * @return the result of the database query
     */
    public Page<PartialBag> findViewAsPage(BagFilter filter) {
        JPAQuery<Bag> count = getJPAQueryFactory().selectFrom(QBag.bag).where(filter.getQuery());
        return PageableExecutionUtils.getPage(
                partialViews(filter),
                filter.createPageRequest(),
                count::fetchCount);
    }

    private JPAQuery<?> partialQuery(BagFilter filter) {
        QBag bag = QBag.bag;
        QNode node = new QNode(DISTRIBUTION_IDENTIFIER);
        QDepositor depositor = QDepositor.depositor;
        QBagDistribution distribution = QBagDistribution.bagDistribution;
        return getJPAQueryFactory().from(bag)
                .innerJoin(bag.depositor, depositor)
                .leftJoin(bag.distributions, distribution)
                .on(distribution.status.eq(BagDistributionStatus.REPLICATE))
                .leftJoin(distribution.node, node)
                .where(filter.getQuery())
                .orderBy(filter.getOrderSpecifier())
                .restrict(filter.getRestriction());
    }

    /**
     * Retrieve a single {@link Bag} from the database projected onto a {@link CompleteBag}. This
     * will create a view which maps to the API model.
     *
     * @param id the id of the {@link Bag} to query
     * @return the {@link CompleteBag} query projection
     */
    public CompleteBag findCompleteView(Long id) {
        QBag bag = QBag.bag;
        QNode node = new QNode(DISTRIBUTION_IDENTIFIER);
        QDepositor depositor = QDepositor.depositor;
        QBagDistribution distribution = QBagDistribution.bagDistribution;

        // originally I wanted to join each staging area separately but this was causing some
        // issues. It might be better to map each staging area based on the type of file, as noted
        // when creating the constructor.
        QDataFile dataFile = QDataFile.dataFile;
        QStagingStorage storage = QStagingStorage.stagingStorage;

        Map<Long, CompleteBag> transform = getJPAQueryFactory()
                .from(bag)
                .innerJoin(bag.depositor, depositor)
                .leftJoin(bag.distributions, distribution)
                .on(distribution.status.eq(BagDistributionStatus.REPLICATE))
                .leftJoin(distribution.node, node)

                // push to function ?
                .leftJoin(bag.storage, storage)
                .on(storage.active.isTrue())
                .leftJoin(storage.file, dataFile)

                .where(bag.id.eq(id))
                .transform(GroupBy.groupBy(bag.id)
                        .as(completeProjection()));

        return transform.get(id);
    }

}
