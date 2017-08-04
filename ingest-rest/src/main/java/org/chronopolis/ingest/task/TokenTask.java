package org.chronopolis.ingest.task;

import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.TokenStagingProperties;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
 * todo: when the TokenRunner is updated to no longer create tokens, we may be able to skip
 *       that work and simply spawn token writers here
 *
 * Created by shake on 2/6/2015.
 */
@Component
@EnableScheduling
@EnableConfigurationProperties(AceConfiguration.class)
public class TokenTask {
    private final Logger log = LoggerFactory.getLogger(TokenTask.class);

    private final TokenRepository tokenRepository;
    private final BagRepository repository;
    private final AceConfiguration ace;
    private final TokenStagingProperties properties;
    private final TrackingThreadPoolExecutor<Bag> tokenExecutor;

    @Autowired
    public TokenTask(TokenRepository tokenRepository,
                     BagRepository repository,
                     AceConfiguration ace,
                     TokenStagingProperties properties,
                     TrackingThreadPoolExecutor<Bag> tokenExecutor) {
        this.tokenRepository = tokenRepository;
        this.repository = repository;
        this.ace = ace;
        this.properties = properties;
        this.tokenExecutor = tokenExecutor;
    }

    @Scheduled(cron = "${ingest.cron.tokens:0 */30 * * * *}")
    public void tokenize() {
        log.info("Searching for bags to tokenize");

        Collection<Bag> bags = repository.findByStatus(BagStatus.INITIALIZED);
        log.debug("Submitting {} bags", bags.size());
        for (Bag bag : bags) {
            TokenRunner runner = new TokenRunner(bag,
                    ace.getIms(),
                    "/dev/null",
                    properties.getPosix().getPath(),
                    repository,
                    tokenRepository);
            tokenExecutor.submitIfAvailable(runner, bag);
        }

    }

}
