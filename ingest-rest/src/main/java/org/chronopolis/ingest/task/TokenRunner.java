package org.chronopolis.ingest.task;

import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import org.chronopolis.common.ace.Tokenizer;
import org.chronopolis.ingest.TokenCallback;
import org.chronopolis.ingest.TokenWriter;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.models.AceToken;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Set;

import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Like BladeRunner, but with Tokens
 * <p/>
 * Created by shake on 2/27/15.
 */
public class TokenRunner implements Runnable {
    private final Logger log = LoggerFactory.getLogger(TokenRunner.class);

    private Bag bag;
    private String bagStage;
    private String tokenStage;
    private BagRepository repository;
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
    }


    @Override
    public void run() {
        Collection<AceToken> tokens = tokenRepository.findByBagID(bag.getID());

        // We have 3 states we check for:
        // * if there are less tokens than the number of files in the bag, tokenize the bag
        //   * there's a chance no tokens have been made, in which case
        //     the filter returns an empty set
        // * if tokenization is complete, update the status of the bag
        // TODO: Send email on failures
        if (tokens.size() < bag.getTotalFiles()) {
            log.info("Starting tokenizer for bag {}", bag.getName());

            // Setup everything we need
            Path toBag = Paths.get(bagStage, bag.getLocation());
            TokenCallback callback = new TokenCallback(tokenRepository, bag);
            Tokenizer tokenizer = new Tokenizer(toBag, bag.getFixityAlgorithm(), callback);

            try {
                tokenizer.tokenize(filter(tokens));
                String tagDigest = tokenizer.getTagManifestDigest();
                log.info("Captured {} as the tagmanifest digest for {}",
                        tagDigest,
                        bag.getName());
                bag.setTagManifestDigest(tagDigest);
            } catch (IOException e) {
                log.error("Error tokenizing: ", e);
            } catch (InterruptedException e) {
                log.error("Interrupted", e);
            }
        } else if (tokens.size() == bag.getTotalFiles()) {
            // TODO: May want to decouple this
            log.info("Writing tokens for bag {}", bag.getName());
            boolean written = writeTokens(bag, tokens);

            if (written) {
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

    private Set<Path> filter(Collection<AceToken> tokens) {
        Set<Path> filter = Sets.newHashSet();
        for (final AceToken token : tokens) {
            filter.add(Paths.get(token.getFilename()));
        }
        return filter;
    }

    /**
     * Write a token to a file identified by the bag name and date
     *
     * @param bag
     * @param tokens
     * @return
     */
    private boolean writeTokens(Bag bag, Collection<AceToken> tokens) {
        Path stage = Paths.get(tokenStage);
        Path dir = stage.resolve(bag.getDepositor());
        if (!dir.toFile().exists()) {
            dir.toFile().mkdirs();
        }

        DateTimeFormatter formatter = ISODateTimeFormat.date().withZoneUTC();
        String filename = bag.getName() + formatter.print(new DateTime());
        Path store = dir.resolve(filename);
        try (OutputStream os = Files.newOutputStream(store, CREATE)) {
            String ims = "ims.umiacs.umd.edu";
            HashingOutputStream hos = new HashingOutputStream(Hashing.sha256(), os);
            TokenWriter writer = new TokenWriter(hos, ims);

            for (AceToken token : tokens) {
                // Make sure we have a leading /
                if (!token.getFilename().startsWith("/")) {
                    token.setFilename("/" + token.getFilename());
                }

                writer.startToken(token);
                writer.addIdentifier(token.getFilename());
                writer.writeTokenEntry();
            }

            // The stream will close on it's own, but call this anyways
            writer.close();
            bag.setTokenDigest(writer.getTokenDigest());
            log.info("TokenStore Digest for bag {}: {}", bag.getID(), writer.getTokenDigest());
        } catch (IOException ex) {
            log.error("Error writing manifest {} ", ex);
            return false;
        }
        log.info("Finished writing tokens");
        bag.setTokenLocation(stage.relativize(store).toString());

        return true;
    }


}
