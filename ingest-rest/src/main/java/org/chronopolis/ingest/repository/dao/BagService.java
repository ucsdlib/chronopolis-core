package org.chronopolis.ingest.repository.dao;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QDepositor;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.IngestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

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

    public Bag create(String creator,
                      IngestRequest request,
                      StorageRegion region,
                      Set<Node> replicatingNodes) {
        String name = request.getName();
        String namespace = request.getDepositor();

        BagSearchCriteria criteria = new BagSearchCriteria()
                .withName(name)
                .withDepositor(namespace);

        Bag bag = find(criteria);
        if (bag != null) {
            // return a 409 instead?
            log.debug("Bag {} exists from depositor {}, skipping creation", name, namespace);
            return bag;
        }
        JPAQueryFactory factory = new JPAQueryFactory(entityManager);
        Depositor depositor = factory.selectFrom(QDepositor.depositor)
                .where(QDepositor.depositor.namespace.eq(namespace))
                .fetchOne();

        log.debug("Received ingest request {}", request);
        Long size = request.getSize();
        Long totalFiles = request.getTotalFiles();

        bag = new Bag(name, depositor);
        bag.setSize(size);
        bag.setTotalFiles(totalFiles);
        bag.setCreator(creator);

        // do we want fixity information on create? (or done later?)
        StagingStorage storage = new StagingStorage();
        storage.setRegion(region);
        storage.setActive(true);
        storage.setSize(size);
        storage.setTotalFiles(totalFiles);
        storage.setPath(request.getLocation());
        bag.setBagStorage(storage);

        if (request.getRequiredReplications() > 0) {
            bag.setRequiredReplications(request.getRequiredReplications());
        }

        createDistributions(bag, replicatingNodes);
        save(bag);

        return bag;
    }

    private void createDistributions(Bag bag, Set<Node> replicatingNodes) {
        // how to log errant nodes?
        for (Node node : replicatingNodes) {
            if (node != null) {
                log.debug("Creating requested dist record for {}", node.username);
                bag.addDistribution(node, DISTRIBUTE);
            }
        }
    }

}
