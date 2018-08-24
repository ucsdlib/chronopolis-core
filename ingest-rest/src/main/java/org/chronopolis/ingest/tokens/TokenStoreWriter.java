package org.chronopolis.ingest.tokens;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.common.io.CountingOutputStream;
import org.chronopolis.common.storage.TokenStagingProperties;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.ingest.repository.criteria.AceTokenSearchCriteria;
import org.chronopolis.ingest.repository.dao.BagService;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.TokenStore;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.enums.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import static java.nio.file.StandardOpenOption.CREATE;
import static org.chronopolis.ingest.api.Params.SORT_ID;
import static org.chronopolis.rest.models.enums.FixityAlgorithm.SHA_256;

/**
 * Runnable task which can write a TokenStore
 *
 * TODO: Configurable names for TokenStores
 * TODO: Test various collection names to see how this can break
 *
 * @author shake
 */
public class TokenStoreWriter implements Runnable {
    private final Logger log = LoggerFactory.getLogger(TokenStoreWriter.class);

    private final Bag bag;
    private final StorageRegion region;
    private final BagService bagService;
    private final TokenStagingProperties properties;
    private final SearchService<AceToken, Long, TokenRepository> tokenService;

    public TokenStoreWriter(Bag bag,
                            StorageRegion region,
                            TokenStagingProperties properties,
                            BagService bagService,
                            SearchService<AceToken, Long, TokenRepository> tokenService) {
        this.bag = bag;
        this.region = region;
        this.properties = properties;
        this.bagService = bagService;
        this.tokenService = tokenService;
    }

    @Override
    public void run() {
        Long bagId = bag.getId();
        String name = bag.getName();
        String depositor = bag.getDepositor().getNamespace();

        StagingStorage storage = new StagingStorage();
        storage.setRegion(region);

        Path root = Paths.get(properties.getPosix().getPath());
        Path dir = root.resolve(depositor);
        if (!dir.toFile().exists()) {
            dir.toFile().mkdirs();
        }

        int page = 0;
        int size = 1000; // todo: determine best size
        Pageable pageable = new PageRequest(page, size, Sort.DEFAULT_DIRECTION, SORT_ID);
        // use the offset to be consistent with the date time we write to the db
        DateTimeFormatter format = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);

        String filename = name + "_" + format.format(ZonedDateTime.now());
        Path store = dir.resolve(filename);
        try (OutputStream os = Files.newOutputStream(store, CREATE);
             HashingOutputStream hos = new HashingOutputStream(Hashing.sha256(), os);
             CountingOutputStream cos = new CountingOutputStream(hos)) {
            TokenWriter writer = new TokenWriter(cos);

            boolean next = true;
            while (next) {
                log.debug("[{}] Iterating page # {} size {} offset {}", bag.getName(),
                        pageable.getPageNumber(), pageable.getPageSize(), pageable.getOffset());
                Page<AceToken> tokens = tokenService.findAll(
                        new AceTokenSearchCriteria().withBagId(bag.getId()),
                        pageable);

                for (AceToken token : tokens) {
                    String tokenFilename = token.getFile().getFilename();
                    log.trace("[{}:{}] Writing token", bag.getId(), tokenFilename);

                    writer.startToken(token);
                    writer.addIdentifier(tokenFilename.startsWith("/")
                            ? tokenFilename
                            : "/" + tokenFilename);
                    writer.writeTokenEntry();
                }

                next = tokens.hasNext();
                pageable = tokens.nextPageable();
            }

            // The stream will close on it's own, but call this anyways
            writer.close();

            Long count = cos.getCount();
            String hash = hos.hash().toString();
            log.info("[Bag {}] Wrote TokenStore(size={},digest={})", bagId, count, hash);
            TokenStore tokenStore = new TokenStore();
            tokenStore.setBag(bag);
            tokenStore.setSize(count);
            tokenStore.setFilename(filename);
            tokenStore.getFixities().add(
                    new Fixity(ZonedDateTime.now(), hash, SHA_256.getCanonical()));

            storage.setBag(bag);
            storage.setSize(count);
            storage.setActive(true);
            storage.setTotalFiles(1L);
            storage.setFile(tokenStore);
            storage.setPath(root.relativize(store).toString());

            bag.getFiles().add(tokenStore);
            bag.getStorage().add(storage);
            bag.setStatus(BagStatus.TOKENIZED);
            bagService.save(bag);
        } catch (Exception ex) { // not to happy about the catch all but there are multiple
                                 // exceptions which can happen
            log.error("Error writing token store {}", store, ex);
        }

    }
}
