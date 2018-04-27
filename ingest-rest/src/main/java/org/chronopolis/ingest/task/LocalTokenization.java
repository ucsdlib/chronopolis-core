package org.chronopolis.ingest.task;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.storage.StagingStorageModel;
import org.chronopolis.tokenize.BagProcessor;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Task for processing bags in our local staging area which need to be tokenized
 *
 * @author shake
 */
@Component
@EnableScheduling
@Profile("local-tokenizer")
public class LocalTokenization {

    private final Logger log = LoggerFactory.getLogger(LocalTokenization.class);

    private final PagedDAO dao;
    private final TokenWorkSupervisor tws;
    private final BagStagingProperties properties;
    private final TrackingThreadPoolExecutor<Bag> executor;
    private final Collection<Predicate<ManifestEntry>> predicates;

    public LocalTokenization(PagedDAO dao,
                             TokenWorkSupervisor tws,
                             BagStagingProperties properties,
                             TrackingThreadPoolExecutor<Bag> executor,
                             Collection<Predicate<ManifestEntry>> predicates) {
        log.info("Creating tokenizer");
        this.dao = dao;
        this.tws = tws;
        this.properties = properties;
        this.executor = executor;
        this.predicates = predicates;
    }

    @Scheduled(cron = "${ingest.cron.tokenize:0 */1 * * * *}")
    public void searchForBags() {
        Posix staging = properties.getPosix();

        BooleanExpression ingestStorage = QBag.bag.bagStorage.any().region.id.eq(staging.getId());

        // Would like to do a paged list but for now this will be ok
        List<Bag> bags = dao.findAll(QBag.bag, ingestStorage.and(
                QBag.bag.status.eq(BagStatus.DEPOSITED)));
        log.debug("Found {} bags for tokenization", bags.size());
        JPAQueryFactory queryFactory = dao.getJPAQueryFactory();
        for (Bag bag : bags) {
            final Long count = queryFactory.selectFrom(QAceToken.aceToken)
                    .where(QAceToken.aceToken.bag.id.eq(bag.getId()))
                    .fetchCount();

            log.trace("[{}] Submitting: {}", bag.getName(), count < bag.getTotalFiles());
            Optional<StagingStorage> storage = bag.getBagStorage().stream()
                    .filter(StagingStorage::isActive)
                    .findFirst();

            storage.map(s -> toModel(bag, s))
                    .filter(s -> count < s.getTotalFiles())
                    .map(model -> new BagProcessor(model, predicates, properties, tws))
                    .ifPresent(processor -> executor.submitIfAvailable(processor, bag));
        }

    }

    /**
     * Map a Bag + BagStorage to a BagModel
     *
     * @param bag        the bag
     * @param bagStorage the active bag storage
     * @return the bag model
     */
    private org.chronopolis.rest.models.Bag toModel(Bag bag, StagingStorage bagStorage) {
        StagingStorageModel storage = new StagingStorageModel()
                .setPath(bagStorage.getPath())
                .setSize(bagStorage.getSize())
                .setActive(bagStorage.isActive())
                .setRegion(bagStorage.getRegion().getId())
                .setTotalFiles(bagStorage.getTotalFiles());

        return new org.chronopolis.rest.models.Bag()
                .setId(bag.getId())
                .setName(bag.getName())
                .setSize(bag.getSize())
                .setBagStorage(storage)
                .setStatus(bag.getStatus())
                .setCreator(bag.getCreator())
                .setCreatedAt(bag.getCreatedAt())
                .setUpdatedAt(bag.getUpdatedAt())
                .setTotalFiles(bag.getTotalFiles())
                .setReplicatingNodes(bag.getReplicatingNodes())
                .setDepositor(bag.getDepositor().getNamespace());
    }

}
