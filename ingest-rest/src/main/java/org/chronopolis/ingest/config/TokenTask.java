package org.chronopolis.ingest.config;

import com.google.common.collect.Sets;
import org.chronopolis.common.ace.Tokenizer;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.TokenCallback;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.models.AceToken;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

/**
 * Created by shake on 2/6/2015.
 */
@Component
@EnableScheduling
public class TokenTask {
    private final Logger log = LoggerFactory.getLogger(TokenTask.class);

    // TODO: Don't use this as a way to test if we're already making tokens...
    private static boolean tokening = false;

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    BagRepository repository;

    @Autowired
    IngestSettings settings;

    @Scheduled(cron = "0 */5 * * * *")
    public void tokenize() {
        if (tokening) {
            log.info("Already creating tokens, skipping this run");
            return;
        }

        log.info("Creating tokens");
        tokening = true;
        Collection<Bag> bags = repository.findByStatus(BagStatus.STAGED);
        for (Bag bag : bags) {
            // TODO: Should just be part of the bag
            Collection<AceToken> tokens = tokenRepository.findByBagID(bag.getID());

            // Setup everything we need
            TokenCallback callback = new TokenCallback(tokenRepository, bag);
            Path toBag = Paths.get(settings.getBagStage(), bag.getLocation());
            Tokenizer tokenizer = new Tokenizer(toBag, bag.getFixityAlgorithm(), callback);

            // We have 3 states we check for:
            // * if there are no tokens, start from the beginning
            // * if there are less tokens than the number of files in the bag, do a partial tokenization
            // * if tokenization is complete, update the status of the bag
            if (tokens.size() == 0) {
                boolean complete = true;
                try {
                    tokenizer.tokenize(Sets.<Path>newHashSet());
                } catch (IOException e) {
                    log.error("Error tokenizing: ", e);
                } catch (InterruptedException e) {
                    log.error("Interrupted", e);
                }
            } else if (tokens.size() < bag.getTotalFiles()) {

            } else if (tokens.size() == bag.getTotalFiles()) {
                log.info("Updating status of {}", bag.getName());
                bag.setStatus(BagStatus.TOKENIZED);
            }

            log.debug("Finished tokenizing {}", bag.getName());
        }
        tokening = false;

    }

}
