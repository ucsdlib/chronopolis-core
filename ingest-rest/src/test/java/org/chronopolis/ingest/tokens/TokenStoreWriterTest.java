package org.chronopolis.ingest.tokens;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.common.storage.TokenStagingProperties;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.StorageRegionRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.criteria.StorageRegionSearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = JpaContext.class)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createBagsWithTokens.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sql/deleteBagsWithTokens.sql")
})
public class TokenStoreWriterTest extends IngestTest {

    // Beans created by spring
    @Autowired private BagRepository bagRepository;
    @Autowired private TokenRepository tokenRepository;
    @Autowired private StorageRegionRepository regionRepository;

    // Our search services which we need to create
    private SearchService<Bag, Long, BagRepository> bagService;
    private SearchService<AceToken, Long, TokenRepository> tokenService;
    private SearchService<StorageRegion, Long, StorageRegionRepository> storageRegionService;

    private TokenStagingProperties properties;

    @Before
    public void setup() {
        properties = new TokenStagingProperties()
                .setPosix(new Posix().setId(1L).setPath(System.getProperty("chron.stage.tokens")));
        bagService = new SearchService<>(bagRepository);
        tokenService = new SearchService<>(tokenRepository);
        storageRegionService = new SearchService<>(regionRepository);
    }

    @Test
    public void testWriteTokens() throws Exception {
        // Init the TokenStoreWriter and run it
        Bag b = bagService.find(new BagSearchCriteria().withId(3L));
        StorageRegion region = storageRegionService.find(new StorageRegionSearchCriteria().withId(properties.getPosix().getId()));
        String stage = properties.getPosix().getPath();
        TokenStoreWriter writer = new TokenStoreWriter(b, region, properties, bagService, tokenService);
        writer.run();

        // Refresh the bag from the db
        Bag updated = bagService.find(new BagSearchCriteria().withId(3L));
        Assert.assertNotNull(updated.getTokenStorage());
        Assert.assertNotNull(updated.getTokenStorage().getFixities());
        Assert.assertNotEquals(0, updated.getTokenStorage().getFixities().size());

        // assert that the file exists
        Path tokens = Paths.get(stage, updated.getTokenStorage().getPath());
        Assert.assertEquals(true, java.nio.file.Files.exists(tokens));

        // the recorded storage information is correct
        StagingStorage staging = updated.getTokenStorage();
        Assert.assertEquals(tokens.toFile().length(), staging.getSize());
        Assert.assertEquals(1, staging.getTotalFiles());

        // the hash value is correct
        Set<Fixity> fixities = updated.getTokenStorage().getFixities();
        HashCode hash = Files.asByteSource(tokens.toFile()).hash(Hashing.sha256());
        boolean fixityMatch = fixities.stream()
                .anyMatch(fixity -> fixity.getValue().equalsIgnoreCase(hash.toString()));
        Assert.assertTrue(fixityMatch);
    }
}