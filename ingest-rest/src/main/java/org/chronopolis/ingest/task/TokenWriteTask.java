package org.chronopolis.ingest.task;

import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.TokenStagingProperties;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.StorageRegionRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.criteria.StorageRegionSearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.ingest.tokens.TokenStoreWriter;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.BagStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import static org.chronopolis.ingest.api.Params.SORT_ID;

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

    private final SearchService<Bag, Long, BagRepository> bags;
    private final SearchService<AceToken, Long, TokenRepository> tokens;
    private final SearchService<StorageRegion, Long, StorageRegionRepository> regions;

    @Autowired
    public TokenWriteTask(TokenStagingProperties properties,
                          TrackingThreadPoolExecutor<Bag> tokenExecutor,
                          SearchService<Bag, Long, BagRepository> bags,
                          SearchService<AceToken, Long, TokenRepository> tokens,
                          SearchService<StorageRegion, Long, StorageRegionRepository> regions) {
        this.properties = properties;
        this.tokenExecutor = tokenExecutor;
        this.bags = bags;
        this.tokens = tokens;
        this.regions = regions;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void searchForTokenizedBags() {
        StorageRegionSearchCriteria srCriteria = new StorageRegionSearchCriteria()
                .withId(properties.getPosix().getId());
        StorageRegion region = regions.find(srCriteria);

        BagSearchCriteria criteria = new BagSearchCriteria()
                .withStatus(BagStatus.TOKENIZED);

        // see if we need to iterate pages or not
        bags.findAll(criteria, new PageRequest(0, 50, Sort.DEFAULT_DIRECTION, SORT_ID))
                .forEach(bag -> tokenExecutor.submitIfAvailable(new TokenStoreWriter(bag, region, properties, bags, tokens), bag));
    }

}
