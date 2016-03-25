package org.chronopolis.ingest;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.entities.Bag;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Test that a file gets written.
 *
 * Only assert:
 *  - location of the store is correct
 *  - digest of the store is correct
 *
 *  Make no assertions on the validity of the store
 *
 * Created by shake on 8/27/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createBagsWithTokens.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sql/deleteBagsWithTokens.sql")
})
public class TokenFileWriterTest extends IngestTest {

    @Autowired BagRepository br;
    @Autowired TokenRepository tr;
    @Autowired IngestSettings settings;

    @Test
    public void testWriteTokens() throws Exception {
        Bag b = br.findOne(Long.valueOf(3));
        String stage = settings.getTokenStage();
        TokenFileWriter writer = new TokenFileWriter(stage, tr);

        boolean written = writer.writeTokens(b);

        // the process completed successfully
        Assert.assertEquals(true, written);

        // assert that the file exists
        Path tokens = Paths.get(stage, b.getTokenLocation());
        Assert.assertEquals(true, java.nio.file.Files.exists(tokens));

        // the hash value is correct
        HashCode hash = Files.hash(tokens.toFile(), Hashing.sha256());
        Assert.assertEquals(b.getTokenDigest(), hash.toString());
    }
}