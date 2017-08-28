package org.chronopolis.tokenize;

import edu.umiacs.ace.ims.api.RequestBatchCallback;
import org.chronopolis.common.ace.Tokenizer;
import org.chronopolis.rest.api.TokenAPI;
import org.chronopolis.rest.entities.Bag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * Like BladeRunner, but with Tokens
 *
 * @author shake
 */
@Deprecated
public class TokenizerRunner implements Runnable {

    /**
     * Factory class to make testing easier.
     */
    static class Factory {
        Runnable makeTokenRunner(Bag b,
                                 String ims,
                                 String bagStage,
                                 TokenAPI tokens) {
            return new TokenizerRunner(b, ims, bagStage, tokens);
        }

        Tokenizer makeTokenizer(Path path, Bag bag, String ims, RequestBatchCallback callback) {
            // todo: find a way to handle the fixity algorithm... maybe a default and preferred
            return new Tokenizer(path, "SHA-256", ims, callback);
        }
    }


    private final Logger log = LoggerFactory.getLogger(TokenizerRunner.class);

    private final Bag bag;
    private final String ims;
    private final String bagStage;
    private final TokenAPI tokens;
    private TokenizerRunner.Factory factory;

    public TokenizerRunner(Bag bag,
                           String ims,
                           String bagStage,
                           TokenAPI tokens) {
        this.bag = bag;
        this.ims = ims;
        this.bagStage = bagStage;
        this.tokens = tokens;
        this.factory = new TokenizerRunner.Factory();
    }

    @Override
    public void run() {
        log.warn("TokenRunner.run is deprecated");
    }

    /*
    @Override
    public void run() {
        // TODO: Send email on failures
        // TODO: If filter contains tagmanifest, check for orphans
        // TODO: Move out of ingest
        log.info("Starting tokenizer for bag {}", bag.getName());

        // Setup everything we need
        Path bagPath = Paths.get(bagStage, bag.getBagStorage().getPath());
        Filter<String> filter = new HttpFilter(bag.getId(), tokens);
        TokenCallback callback = new TokenCallback(bag, tokens);
        Tokenizer tokenizer = factory.makeTokenizer(bagPath, bag, ims, callback);

        try {
            tokenizer.tokenize(filter);
        } catch (IOException e) {
            log.error("Error tokenizing: ", e);
        } catch (InterruptedException e) {
            log.error("Interrupted", e);
        }

        log.debug("Exiting TokenRunner for {}", bag.getName());
    }
    */


}
