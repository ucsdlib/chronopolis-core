package org.chronopolis.ingest.repository.dao;

import com.google.common.collect.ImmutableList;
import org.chronopolis.ingest.support.BagCreateResult;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagDistributionStatus;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.depositor.QDepositor;
import org.chronopolis.rest.models.create.BagCreate;
import org.chronopolis.rest.models.enums.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import java.util.Optional;

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
                getJPAQueryFactory().selectFrom(QDepositor.depositor)
                        .where(QDepositor.depositor.namespace.eq(namespace))
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

}
