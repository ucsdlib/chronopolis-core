package org.chronopolis.ingest.task;

import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.TokenStagingProperties;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.ingest.repository.criteria.StorageRegionSearchCriteria;
import org.chronopolis.ingest.repository.dao.BagService;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.ingest.repository.dao.StorageRegionService;
import org.chronopolis.ingest.tokens.TokenStoreWriter;
import org.chronopolis.rest.kot.entities.AceToken;
import org.chronopolis.rest.kot.entities.Bag;
import org.chronopolis.rest.kot.entities.storage.StorageRegion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Task to spawn new TokenWriter threads or whatever
 *
 * @author shake
 */
@Component
@EnableScheduling
public class TokenWriteTask {

    private final TokenStagingProperties properties;
    private final TrackingThreadPoolExecutor<Bag> tokenExecutor;

    private final BagService bags;
    private final SearchService<AceToken, Long, TokenRepository> tokens;
    private final StorageRegionService regions;

    @Autowired
    public TokenWriteTask(TokenStagingProperties properties,
                          TrackingThreadPoolExecutor<Bag> tokenExecutor,
                          BagService bags,
                          SearchService<AceToken, Long, TokenRepository> tokens,
                          StorageRegionService regions) {
        this.properties = properties;
        this.tokenExecutor = tokenExecutor;
        this.bags = bags;
        this.tokens = tokens;
        this.regions = regions;
    }

    @Scheduled(cron = "${ingest.cron.tokens: 0 */10 * * * *}")
    public void searchForTokenizedBags() {
        StorageRegionSearchCriteria srCriteria = new StorageRegionSearchCriteria()
                .withId(properties.getPosix().getId());
        StorageRegion region = regions.find(srCriteria);

        bags.getBagsWithAllTokens().forEach(bag -> {
            TokenStoreWriter writer = new TokenStoreWriter(bag, region, properties, bags, tokens);
            tokenExecutor.submitIfAvailable(writer, bag);
        });
    }

}
