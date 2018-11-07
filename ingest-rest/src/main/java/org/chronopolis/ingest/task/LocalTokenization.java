package org.chronopolis.ingest.task;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.rest.api.IngestApiProperties;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.serializers.ExtensionsKt;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.tokenize.BagProcessor;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@Profile("!disable-tokenizer")
@EnableConfigurationProperties(IngestApiProperties.class)
public class LocalTokenization {

    private final Logger log = LoggerFactory.getLogger(LocalTokenization.class);

    private final PagedDao dao;
    private final TokenWorkSupervisor tws;
    private final IngestApiProperties apiProperties;
    private final BagStagingProperties properties;
    private final TrackingThreadPoolExecutor<Bag> executor;
    private final Collection<Predicate<ManifestEntry>> predicates;

    public LocalTokenization(PagedDao dao,
                             TokenWorkSupervisor tws,
                             IngestApiProperties apiProperties,
                             BagStagingProperties properties,
                             TrackingThreadPoolExecutor<Bag> executor,
                             Collection<Predicate<ManifestEntry>> predicates) {
        log.info("Creating tokenizer");
        this.dao = dao;
        this.tws = tws;
        this.properties = properties;
        this.apiProperties = apiProperties;
        this.executor = executor;
        this.predicates = predicates;
    }

    @Scheduled(cron = "${ingest.cron.tokenize:0 0 * * * *}")
    public void searchForBags() {
        Posix staging = properties.getPosix();

        String creator = apiProperties.getUsername();
        if (creator == null) {
            creator = "admin";
        }

        // todo: enforce that the storage has a 'BAG' file for validation
        BooleanExpression ingestStorage = QBag.bag.storage.any().region.id.eq(staging.getId());

        // Would like to do a paged list but for now this will be ok
        List<Bag> bags = dao.findAll(QBag.bag, ingestStorage
                .and(QBag.bag.status.eq(BagStatus.DEPOSITED))
                .and(QBag.bag.creator.eq(creator)));
        log.debug("Found {} bags for tokenization", bags.size());
        JPAQueryFactory queryFactory = dao.getJPAQueryFactory();
        for (Bag bag : bags) {
            final Long count = queryFactory.selectFrom(QAceToken.aceToken)
                    .where(QAceToken.aceToken.bag.id.eq(bag.getId()))
                    .fetchCount();

            log.trace("[{}] Submitting: {}", bag.getName(), count < bag.getTotalFiles());
            Optional<StagingStorage> storage = bag.getStorage().stream()
                    .filter(StagingStorage::isActive)
                    .findFirst();

            storage.map(s -> ExtensionsKt.model(bag))
                    .filter(s -> count < s.getTotalFiles())
                    .map(model -> new BagProcessor(model, predicates, properties, tws))
                    .ifPresent(processor -> executor.submitIfAvailable(processor, bag));
        }

    }

}
