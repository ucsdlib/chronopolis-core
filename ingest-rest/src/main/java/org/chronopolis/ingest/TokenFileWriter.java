package org.chronopolis.ingest;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.file.StandardOpenOption.CREATE;

/**
 * Class which writes tokenstores
 * TODO: Runnable?
 *
 * Created by shake on 8/26/15.
 */
public class TokenFileWriter {
    private final Logger log = LoggerFactory.getLogger(TokenFileWriter.class);

    private Path stage;
    private TokenRepository repository;

    public TokenFileWriter(String stage, TokenRepository repository) {
        this.stage = Paths.get(stage);
        this.repository = repository;
    }

    /**
     * Write a token to a file identified by the bag name and date
     * TODO: Remove various magic values
     *
     * @param bag
     * @return
     */
    public boolean writeTokens(Bag bag) {
        Long bagId = bag.getId();
        String name = bag.getName();
        String depositor = bag.getDepositor();

        Path dir = stage.resolve(depositor);
        if (!dir.toFile().exists()) {
            dir.toFile().mkdirs();
        }

        Pageable pageable = new PageRequest(0, 1000);
        DateTimeFormatter formatter = ISODateTimeFormat.date().withZoneUTC();

        // TODO: Configurable names for tokens
        // TODO: Slashes in a filename will break the writer, should do mkdirs on the last parent dir
        String filename = name + "_" + formatter.print(new DateTime());
        Path store = dir.resolve(filename);
        try (OutputStream os = Files.newOutputStream(store, CREATE)) {
            String ims = "ims.umiacs.umd.edu";
            HashingOutputStream hos = new HashingOutputStream(Hashing.sha256(), os);
            TokenWriter writer = new TokenWriter(hos, ims);

            boolean next = true;
            while (next) {
                log.debug("Iterating page # {} size {} offset {}",
                        new Object[]{pageable.getPageNumber(), pageable.getPageSize(), pageable.getOffset()});
                Page<AceToken> tokens = repository.findByBagIdOrderByIdAsc(bagId, pageable);

                for (AceToken token : tokens) {
                    log.trace("Writing {}", token.getFilename());
                    // Make sure we have a leading /
                    if (!token.getFilename().startsWith("/")) {
                        token.setFilename("/" + token.getFilename());
                    }

                    writer.startToken(token);
                    writer.addIdentifier(token.getFilename());
                    writer.writeTokenEntry();
                }

                next = tokens.hasNext();
                pageable = tokens.nextPageable();
            }

            // The stream will close on it's own, but call this anyways
            writer.close();
            bag.setTokenDigest(writer.getTokenDigest());
            log.info("TokenStore Digest for bag {}: {}", bagId, writer.getTokenDigest());
        } catch (IOException ex) {
            log.error("Error writing token store {}", store, ex);
            return false;
        }

        log.info("Finished writing tokens");
        bag.setTokenLocation(stage.relativize(store).toString());

        return true;
    }

}
