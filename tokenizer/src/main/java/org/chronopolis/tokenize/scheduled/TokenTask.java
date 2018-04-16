package org.chronopolis.tokenize.scheduled;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.rest.api.BagService;
import org.chronopolis.rest.api.IngestAPIProperties;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.tokenize.BagProcessor;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.chronopolis.tokenize.config.TokenTaskConfiguration;
import org.chronopolis.tokenize.filter.HttpFilter;
import org.chronopolis.tokenize.filter.ProcessingFilter;
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
import java.util.function.Predicate;

/**
 * Basic task to submit bags for tokenization
 * <p>
 * Created by shake on 2/6/2015.
 */
@Component
@EnableScheduling
@EnableConfigurationProperties(BagStagingProperties.class)
public class TokenTask {
    private final Logger log = LoggerFactory.getLogger(TokenTaskConfiguration.TOKENIZER_LOG_NAME);

    private final BagService service;
    private final TokenService tokens;
    private final TokenWorkSupervisor supervisor;
    private final BagStagingProperties properties;
    private final IngestAPIProperties ingestProperties;
    private final TrackingThreadPoolExecutor<Bag> executor;
    private final ProcessingFilter processingFilter;

    @Autowired
    public TokenTask(ServiceGenerator generator,
                     TokenWorkSupervisor supervisor,
                     BagStagingProperties properties,
                     IngestAPIProperties ingestProperties,
                     TrackingThreadPoolExecutor<Bag> executor) {
        this.tokens = generator.tokens();
        this.service = generator.bags();
        this.supervisor = supervisor;
        this.properties = properties;
        this.ingestProperties = ingestProperties;
        this.executor = executor;
        this.processingFilter = new ProcessingFilter(supervisor);
    }

    @Scheduled(cron = "${ingest.cron.tokens:0/30 * * * * *}")
    public void tokenize() {
        log.info("Searching for bags to tokenize");

        // Query ingest API
        // Maybe getMyBags? Can work this out later
        Call<PageImpl<Bag>> bags = service.get(
                ImmutableMap.of("status", BagStatus.DEPOSITED,
                        "creator", ingestProperties.getUsername(),
                        "region_id", properties.getPosix().getId()));
        try {
            Response<PageImpl<Bag>> response = bags.execute();
            if (response.isSuccessful()) {
                log.debug("Found {} bags for tokenization", response.body().getSize());
                for (Bag bag : response.body()) {
                    ImmutableList<Predicate<ManifestEntry>> predicates =
                            ImmutableList.of(processingFilter, new HttpFilter(bag.getId(), tokens));
                    executor.submitIfAvailable(
                            new BagProcessor(bag, predicates, properties, supervisor),
                            bag);
                }
            }
        } catch (IOException e) {
            log.error("Error communicating with the ingest server", e);
        }

    }

}
