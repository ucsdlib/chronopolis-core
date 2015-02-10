package org.chronopolis.ingest.config;

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

    @Autowired
    TokenRepository tokenRepository;

    @Autowired
    BagRepository repository;

    @Autowired
    IngestSettings settings;

    public void tokenize() {
        Collection<Bag> bags = repository.findByStatus(BagStatus.STAGED);
        for (Bag bag : bags) {
            // TODO: Should just be part of the bag
            Collection<AceToken> tokens = tokenRepository.findByBagID(bag.getID());

            // Setup everything we need
            TokenCallback callback = new TokenCallback(tokenRepository, bag);
            Path toBag = Paths.get(settings.getBagStage(), bag.getLocation());
            Tokenizer tokenizer = new Tokenizer(toBag, bag.getFixityAlgorithm(), callback);

            if (tokens.size() == 0) {
                // From the beginning
                try {
                    tokenizer.fullTokenize();
                } catch (IOException e) {
                    log.error("Error tokenizing: ", e);
                } catch (InterruptedException e) {
                    log.error("Interrupted", e);
                }

            } else if (tokens.size() < bag.getTotalFiles()) {
                // Partial tokenization

            }

            log.debug("Finished tokenizing {}", bag.getName());
        }

    }

}
