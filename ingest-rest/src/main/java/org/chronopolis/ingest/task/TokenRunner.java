package org.chronopolis.ingest.task;

import org.chronopolis.common.ace.Tokenizer;
import org.chronopolis.common.util.Filter;
import org.chronopolis.ingest.TokenCallback;
import org.chronopolis.ingest.TokenFileWriter;
import org.chronopolis.ingest.TokenFilter;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Like BladeRunner, but with Tokens
 *
 *
 * Created by shake on 2/27/15.
 */
public class TokenRunner implements Runnable {

    /**
     * Factory class to make testing easier.
     *
     */
    static class Factory {
        Runnable makeTokenRunner(Bag b,
                                 String bagStage,
                                 String tokenStage,
                                 BagRepository bagRepository,
                                 TokenRepository tokenRepository) {
            return new TokenRunner(b, bagStage, tokenStage, bagRepository, tokenRepository);
        }

        Tokenizer makeTokenizer(Path path, Bag bag, TokenCallback callback){
            return new Tokenizer(path, bag.getFixityAlgorithm(), callback);
        }

        TokenFileWriter makeFileWriter(String stage, TokenRepository tr) {
            return new TokenFileWriter(stage, tr);
        }
    }


    private final Logger log = LoggerFactory.getLogger(TokenRunner.class);

    private Bag bag;
    private String bagStage;
    private String tokenStage;
    private BagRepository repository;
    private TokenRunner.Factory factory;
    private TokenRepository tokenRepository;

    public TokenRunner(final Bag bag,
                       final String bagStage,
                       final String tokenStage,
                       final BagRepository repository,
                       final TokenRepository tokenRepository) {
        this.bag = bag;
        this.bagStage = bagStage;
        this.tokenStage = tokenStage;
        this.repository = repository;
        this.tokenRepository = tokenRepository;
        this.factory = new TokenRunner.Factory();
    }


    @Override
    public void run() {
        Long bagId = bag.getId();
        Long size = tokenRepository.countByBagId(bagId);

        // We have 3 states we check for:
        // * if there are less tokens than the number of files in the bag, tokenize the bag
        //   * there's a chance no tokens have been made, in which case
        //     the filter returns an empty set
        // * if tokenization is complete, update the status of the bag
        // TODO: Send email on failures
        // TODO: If filter contains tagmanifest, check for orphans
        // log.debug("{}: Token size: {} Total Files: {}", new Object[]{bag.getName(), tokens.size(), bag.getTotalFiles()});
        if (size < bag.getTotalFiles()) {
            log.info("Starting tokenizer for bag {}", bag.getName());

            // Setup everything we need
            Path toBag = Paths.get(bagStage, bag.getLocation());
            Filter<Path> filter = new TokenFilter(tokenRepository, bagId);
            TokenCallback callback = new TokenCallback(tokenRepository, bag);
            Tokenizer tokenizer = factory.makeTokenizer(toBag, bag, callback);

            try {
                tokenizer.tokenize(filter);
                if (bag.getTagManifestDigest() == null) {
                    String tagDigest = tokenizer.getTagManifestDigest();
                    log.info("Captured {} as the tagmanifest digest for {}",
                            tagDigest,
                            bag.getName());
                    bag.setTagManifestDigest(tagDigest);
                }
            } catch (IOException e) {
                log.error("Error tokenizing: ", e);
            } catch (InterruptedException e) {
                log.error("Interrupted", e);
            }
        } else if (size == bag.getTotalFiles()) {
            // TODO: May want to decouple this
            log.info("Writing tokens for bag {}", bag.getName());
            TokenFileWriter writer = factory.makeFileWriter(tokenStage, tokenRepository);

            if (writer.writeTokens(bag)) {
                log.info("Updating status of {}", bag.getName());
                bag.setStatus(BagStatus.TOKENIZED);
            }

        }
        // If greater, set the bag as an error?

        repository.save(bag);
        log.debug("Exiting TokenRunner for {}", bag.getName());
    }

    public Bag getBag() {
        return bag;
    }


}
