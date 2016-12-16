package org.chronopolis.ingest.task;

import org.chronopolis.common.settings.AceSettings;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Basic task to submit threads for tokenization
 *
 * Works on bags which have finished being INITIALIZED
 * so that they may be replicated
 *
 * Created by shake on 2/6/2015.
 */
@Component
@EnableScheduling
public class TokenTask {
    private final Logger log = LoggerFactory.getLogger(TokenTask.class);

    private final TokenRepository tokenRepository;
    private final BagRepository repository;
    private final IngestSettings settings;
    private final AceSettings ace;
    private final TrackingThreadPoolExecutor<Bag> tokenExecutor;

    @Autowired
    public TokenTask(TokenRepository tokenRepository, BagRepository repository, IngestSettings settings, AceSettings ace, TrackingThreadPoolExecutor<Bag> tokenExecutor) {
        this.tokenRepository = tokenRepository;
        this.repository = repository;
        this.settings = settings;
        this.ace = ace;
        this.tokenExecutor = tokenExecutor;
    }

    @Scheduled(cron = "${ingest.cron.tokens:0 */30 * * * *}")
    public void tokenize() {
        log.info("Searching for bags to tokenize");

        /*
        if (tokenizingThreadPoolExecutor.getActiveCount() > MAX_RUN) {
            log.info("Waiting for executor to finish before starting more tokens");
            return;
        }
        */

        // Might need pagination in the future
        Collection<Bag> bags = repository.findByStatus(BagStatus.INITIALIZED);
        log.debug("Submitting {} bags", bags.size());
        for (Bag bag : bags) {
            TokenRunner runner = new TokenRunner(bag,
                    ace.getImsHost(),
                    settings.getBagStage(),
                    settings.getTokenStage(),
                    repository,
                    tokenRepository);
            tokenExecutor.submitIfAvailable(runner, bag);
        }

    }

}
