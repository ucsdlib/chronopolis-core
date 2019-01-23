package org.chronopolis.ingest.task;

import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagFile;
import org.chronopolis.rest.entities.QDataFile;
import org.chronopolis.rest.entities.serializers.ExtensionsKt;
import org.chronopolis.rest.entities.storage.QStagingStorage;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.tokenize.BagProcessor;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static org.chronopolis.ingest.repository.dao.StagingDao.DISCRIMINATOR_BAG;

/**
 * Task for processing bags in our local staging area which need to be tokenized
 *
 * @author shake
 */
@Component
@EnableScheduling
@ConditionalOnProperty(prefix = "ingest", name = "tokenizer.enabled", havingValue = "true")
public class LocalTokenization {

    private final Logger log = LoggerFactory.getLogger(LocalTokenization.class);

    private final PagedDao dao;
    private final TokenWorkSupervisor tws;
    private final IngestProperties properties;
    private final TrackingThreadPoolExecutor<Bag> executor;
    private final Collection<Predicate<ManifestEntry>> predicates;

    @Autowired
    public LocalTokenization(PagedDao dao,
                             TokenWorkSupervisor tws,
                             IngestProperties properties,
                             TrackingThreadPoolExecutor<Bag> executor,
                             Collection<Predicate<ManifestEntry>> predicates) {
        this.dao = dao;
        this.tws = tws;
        this.properties = properties;
        this.executor = executor;
        this.predicates = predicates;
    }

    @Scheduled(cron = "${ingest.tokenizer.cron:0 0 * * * *}")
    public void searchForBags() {
        IngestProperties.Tokenizer tokenizer = properties.getTokenizer();
        if (!tokenizer.getEnabled()) {
            return;
        }

        Posix staging = tokenizer.getStaging();
        String creator = tokenizer.getUsername();
        if (creator == null) {
            creator = "admin";
        }

        JPAQueryFactory queryFactory = dao.getJPAQueryFactory();
        // bleh... need to rethink some things wrt the properties
        BagStagingProperties bp = new BagStagingProperties().setPosix(staging);

        // Would like to do a paged list but for now this will be ok
        QBag qBag = QBag.bag;
        QBagFile qBagFile = QBagFile.bagFile;
        List<Bag> bags = queryFactory.selectFrom(qBag)
                .innerJoin(qBag.storage, QStagingStorage.stagingStorage)
                .fetchJoin()
                .innerJoin(QStagingStorage.stagingStorage.file, QDataFile.dataFile)
                .on(QStagingStorage.stagingStorage.file.dtype.eq(DISCRIMINATOR_BAG))
                .where(qBag.status.eq(BagStatus.INITIALIZED)
                        .and(qBag.creator.eq(creator))
                        .and(qBag.totalFiles.eq(JPAExpressions.select(qBagFile.count())
                                .from(qBagFile)
                                .where(qBagFile.bag.id.eq(qBag.id))))
                        .and(qBag.storage.size().eq(1))
                        .and(QStagingStorage.stagingStorage.active.isTrue()))
                .fetch();

        log.debug("Found {} bags for tokenization", bags.size());
        for (Bag bag : bags) {
            final Long count = queryFactory.selectFrom(QAceToken.aceToken)
                    .where(QAceToken.aceToken.bag.id.eq(bag.getId()))
                    .fetchCount();

            log.trace("[{}] Submitting: {}", bag.getName(), count < bag.getTotalFiles());
            if (count < bag.getTotalFiles()) {
                BagProcessor processor =
                        new BagProcessor(ExtensionsKt.model(bag), predicates, bp, tws);
                executor.submitIfAvailable(processor, bag);
            }
        }

    }

}
