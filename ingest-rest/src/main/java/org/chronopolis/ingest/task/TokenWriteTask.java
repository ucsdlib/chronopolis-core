package org.chronopolis.ingest.task;

import com.querydsl.jpa.JPAExpressions;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.TokenStagingProperties;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.ingest.tokens.TokenStoreWriter;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QAceToken;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.storage.QStagingStorage;
import org.chronopolis.rest.entities.storage.QStorageRegion;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.enums.BagStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Task to spawn new TokenWriter threads or whatever
 *
 * @author shake
 */
@Component
@EnableScheduling
public class TokenWriteTask {

    private final PagedDao dao;
    private final TokenStagingProperties properties;
    private final TrackingThreadPoolExecutor<Bag> tokenExecutor;

    @Autowired
    public TokenWriteTask(TokenStagingProperties properties,
                          TrackingThreadPoolExecutor<Bag> tokenExecutor,
                          PagedDao dao) {
        this.properties = properties;
        this.tokenExecutor = tokenExecutor;
        this.dao = dao;
    }

    @Scheduled(cron = "${ingest.cron.tokens: 0 */10 * * * *}")
    public void searchForTokenizedBags() {
        StorageRegion region = dao.findOne(QStorageRegion.storageRegion,
                QStorageRegion.storageRegion.id.eq(properties.getPosix().getId()));

        getBagsWithAllTokens().forEach(bag -> {
            TokenStoreWriter writer = new TokenStoreWriter(bag, region, properties, dao);
            tokenExecutor.submitIfAvailable(writer, bag);
        });
    }

    /**
     * Retrieve a List of DEPOSITED Bags which have the same amount of ACE Tokens registered
     * as total files
     * <p>
     * The query is equivalent to:
     * SELECT * FROM bag b
     * WHERE status = 'DEPOSITED' AND
     * total_files = (SELECT count(id) FROM ace_token WHERE bag_id = b.id);
     *
     * @return the List of Bags matching the query
     */
    private List<Bag> getBagsWithAllTokens() {
        QBag bag = QBag.bag;
        QAceToken token = QAceToken.aceToken;
        return dao.getJPAQueryFactory().selectFrom(bag)
                .innerJoin(bag.storage, QStagingStorage.stagingStorage)
                .fetchJoin()
                .where(bag.status.eq(BagStatus.DEPOSITED),
                        bag.totalFiles.eq(
                                JPAExpressions.select(token.id.count())
                                        .from(token)
                                        .where(token.bag.id.eq(bag.id))))
                .fetch();
    }

}
