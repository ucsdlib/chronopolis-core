package org.chronopolis.ingest.task;

import org.chronopolis.common.ace.Tokenizer;
import org.chronopolis.common.util.Filter;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.TokenCallback;
import org.chronopolis.ingest.TokenFileWriter;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.Path;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for the TokenRunner class. We don't worry about actual file writing/tokenizing,
 * just that the appropriate branches get taken
 *
 * Created by shake on 8/26/15.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = JpaContext.class)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createBagsWithTokens.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sql/deleteBagsWithTokens.sql")
})
public class TokenRunnerTest extends IngestTest {

    private final String TAG_MANIFEST_DIGEST = "tag-manifest-digest";
    private final String IMS_HOST = "imstest.umiacs.umd.edu";

    // Beans created on startup
    @Autowired private BagRepository br;
    @Autowired private TokenRepository tr;

    // Our mocks for the various classes we aren't testing
    @Mock private TokenFileWriter writer;
    @Mock private Tokenizer tokenizer;
    @Mock private TokenRunner.Factory factory;

    // Our TokenRunner which gets injected
    @InjectMocks private TokenRunner runner;

    @Test
    public void testRunWithTokenizer() throws Exception {
        Bag b = br.findOne(Long.valueOf(2));
        String bs = System.getProperty("chron.stage.bags");
        String ts = System.getProperty("chron.stage.tokens");

        runner = new TokenRunner(b, IMS_HOST, bs, ts, br, tr);
        MockitoAnnotations.initMocks(this);

        when(factory.makeTokenizer(any(Path.class),
                                   any(Bag.class),
                                   any(String.class),
                                   any(TokenCallback.class)))
                .thenReturn(tokenizer);

        when(tokenizer.getTagManifestDigest())
            .thenReturn(TAG_MANIFEST_DIGEST);

        runner.run();

        verify(tokenizer, times(1)).tokenize(any(Filter.class));
        // verify(tokenizer, times(1)).getTagManifestDigest();

        // todo: This probaby won't be in ingest anymore... all these changes will make merging a pain
        // Assert.assertEquals(TAG_MANIFEST_DIGEST, b.getTagManifestDigest());
    }

    @Test
    public void testRunWithWriter() throws Exception {
        Bag b = br.findOne(Long.valueOf(3));
        String bs = System.getProperty("chron.stage.bags");
        String ts = System.getProperty("chron.stage.tokens");

        runner = new TokenRunner(b, IMS_HOST, bs, ts, br, tr);
        MockitoAnnotations.initMocks(this);

        when(factory.makeFileWriter(any(String.class), any(TokenRepository.class)))
                .thenReturn(writer);

        when(writer.writeTokens(b))
                .thenReturn(true);

        runner.run();

        verify(writer, times(1)).writeTokens(b);

        Assert.assertEquals(BagStatus.TOKENIZED, b.getStatus());
    }

    // test run with exceptions
    //  -> when tokenizer run then throw IOException
    //  -> when tokenizer run then throw InterruptedException
    //  -> verify times(0) getTagManifestDigest

}