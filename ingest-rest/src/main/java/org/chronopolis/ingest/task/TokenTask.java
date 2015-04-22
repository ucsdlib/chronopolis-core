package org.chronopolis.ingest.task;

import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Created by shake on 2/6/2015.
 */
@Component
@EnableScheduling
public class TokenTask {
    private final Logger log = LoggerFactory.getLogger(TokenTask.class);

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    BagRepository repository;

    @Autowired
    IngestSettings settings;

    @Autowired
    TokenThreadPoolExecutor executor;

    @Scheduled(cron = "0 */30 * * * *")
    public void tokenize() {
        log.info("Searching for bags to tokenize");
        if (executor.getActiveCount() > 0) {
            log.info("Waiting for executor to finish before starting more tokens");
            return;
        }

        Collection<Bag> bags = repository.findByStatus(BagStatus.STAGED);
        log.debug("Submitting {} bags", bags.size());
        for (Bag bag : bags) {
            executor.submitBagIfAvailable(bag, settings, repository, tokenRepository);
        }

    }

}
