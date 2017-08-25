package org.chronopolis.tokenize.scheduled;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.storage.BagStagingProperties;
import org.chronopolis.common.util.Filter;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.batch.ChronopolisTokenRequestBatch;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

/**
 * Basic task to submit bags for tokenization
 * <p>
 * Created by shake on 2/6/2015.
 */
@Component
@EnableScheduling
@EnableConfigurationProperties(AceConfiguration.class)
public class TokenTask {
    private final Logger log = LoggerFactory.getLogger(TokenTask.class);

    private final IngestAPI ingest;
    private final AceConfiguration ace;
    private final BagStagingProperties properties;
    private final ChronopolisTokenRequestBatch batch;
    private final TrackingThreadPoolExecutor<Bag> tokenExecutor;

    @Autowired
    public TokenTask(IngestAPI ingest,
                     AceConfiguration ace,
                     BagStagingProperties properties,
                     ChronopolisTokenRequestBatch batch,
                     TrackingThreadPoolExecutor<Bag> tokenExecutor) {
        this.ingest = ingest;
        this.ace = ace;
        this.properties = properties;
        this.batch = batch;
        this.tokenExecutor = tokenExecutor;
    }

    @Scheduled(cron = "${ingest.cron.tokens:0 */30 * * * *}")
    public void tokenize() {
        log.info("Searching for bags to tokenize");

        // Query ingest API
        // Maybe getMyBags? Can work this out later
        Call<PageImpl<Bag>> bags = ingest.getBags(
                ImmutableMap.of("status", BagStatus.DEPOSITED,
                        "region_id", properties.getPosix().getId()));
        try {
            Response<PageImpl<Bag>> response = bags.execute();
            if (response.isSuccessful()) {
                response.body().forEach(this::submit);
            }
        } catch (IOException e) {
            log.error("Error communicating with the ingest server", e);
        }

    }

    /**
     * Submit a bag to Tokenize its files
     * <p>
     * Possibly push this to another class so that we don't duplicate
     * processing which is ongoing
     *
     * @param bag
     */
    private void submit(Bag bag) {
        final String manifestName = "manifest-sha256.txt";
        final String tagmanifestName = "tagmanifest-sha256.txt";

        String root = properties.getPosix().getPath();
        String relative = bag.getBagStorage().getPath();

        // I'm not sure of the best way to handle this, but we only want to continue
        // if we've finished processing the previous manifest. In each case this means
        // we should have 0 items left over from the filter. This makes things a little slow
        // but it's a first pass at how to incorporate the new logic
        long processed = -1;
        for (String name : ImmutableList.of(manifestName, tagmanifestName)) {
            processed = process(bag, root, relative, name);

            if (processed < 0 || processed > 0) {
                break;
            }
        }

        // if processed == 0 && !filter.contains(entry)
        // digest && validate
        // submit

    }

    /**
     * Process a given manifest to create ACE Tokens
     *
     * @param bag
     * @param root
     * @param relative
     * @param name
     */
    private long process(Bag bag, String root, String relative, String name) {
        long count = -1;
        final int PATH_IDX = 1;
        final int DIGEST_IDX = 0;
        Filter<String> filter = null;
        Path manifest = Paths.get(root, relative, name);
        try (Stream<String> lines = Files.lines(manifest)) {
            // trying to work out the logic so that we process the manifest, then tagmanifest, then digest the tagmanifest and tokenize it
            // basically we _should_ be able to get a count of entries created; if that is 0 then the ingest server has all the entries
            // if it is greater than 0 then we were still creating tokens
            // not sure what type of strain this might put on the ingest server but we can do some testing
            count = lines.map(line -> line.split("\\s", 2))
                    .filter(entries -> !filter.contains(entries[PATH_IDX]))
                    .map(entries -> { // push to another class
                        Path file = Paths.get(root, relative, entries[PATH_IDX]);
                        ManifestEntry entry = new ManifestEntry(bag, entries[PATH_IDX], entries[DIGEST_IDX]);
                        try {
                            HashCode hash = com.google.common.io.Files.asByteSource(file.toFile())
                                    .hash(Hashing.sha256());
                            entry.setCalculatedDigest(hash.toString());
                        } catch (IOException e) {
                            // on error maybe we can trigger something so that we don't repeat with the tagmanifest
                            log.warn("Unable to digest {}", entries[PATH_IDX], e);
                        }
                        return entry;
                    })
                    .filter(ManifestEntry::isValid)
                    .peek(batch::add)
                    .count();
        } catch (IOException e) {
            log.error("[{}] Error processing manifest {}", bag.getName(), name);
            log.error("", e);
        }

        return count;
    }

}
