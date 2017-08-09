package org.chronopolis.tokenize.scheduled;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.PageImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;

/**
 * Basic task to submit threads for tokenization
 * <p>
 * Works on bags which have finished being INITIALIZED
 * so that they may be replicated
 * <p>
 * todo: when the TokenRunner is updated to no longer create tokens, we may be able to skip
 * that work and simply spawn token writers here
 * <p>
 * Created by shake on 2/6/2015.
 */
@Component
@EnableScheduling
@EnableConfigurationProperties(AceConfiguration.class)
public class TokenTask {
    private final Logger log = LoggerFactory.getLogger(TokenTask.class);

    private final IngestAPI ingest;
    private final AceConfiguration ace;
    private final BagStagingProperties properties;
    private final TrackingThreadPoolExecutor<Bag> tokenExecutor;

    @Autowired
    public TokenTask(IngestAPI ingest,
                     AceConfiguration ace,
                     BagStagingProperties properties,
                     TrackingThreadPoolExecutor<Bag> tokenExecutor) {
        this.ingest = ingest;
        this.ace = ace;
        this.properties = properties;
        this.tokenExecutor = tokenExecutor;
    }

    @Scheduled(cron = "${ingest.cron.tokens:0 */30 * * * *}")
    public void tokenize() {
        log.info("Searching for bags to tokenize");

        // Query ingest API
        // Maybe getMyBags? Can work this out later
        // Also need the storage region
        Call<PageImpl<org.chronopolis.rest.models.Bag>> bags = ingest.getBags(
                ImmutableMap.of("status", BagStatus.DEPOSITED,
                        "region_id", properties.getPosix().getId()));
        try {
            Response<PageImpl<org.chronopolis.rest.models.Bag>> response = bags.execute();
        } catch (IOException e) {
            log.error("Error communicating with the ingest server", e);
        }

    }

}
