package org.chronopolis.ingest;

import com.google.common.collect.ImmutableList;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import edu.umiacs.ace.exception.StatusCode;
import edu.umiacs.ace.ims.api.RequestBatchCallback;
import edu.umiacs.ace.ims.api.TokenRequestBatch;
import edu.umiacs.ace.ims.ws.ProofElement;
import edu.umiacs.ace.ims.ws.TokenRequest;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.common.ace.Tokenizer;
import org.chronopolis.common.util.Filter;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.entities.AceToken;
import org.chronopolis.rest.entities.Bag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for our tokenizer
 *
 * Created by shake on 8/25/15.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = JpaContext.class)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createBagsWithTokens.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sql/deleteBagsWithTokens.sql")
})
public class TokenizerTest extends IngestTest {
    private final Logger log = LoggerFactory.getLogger(TokenizerTest.class);

    private final int TOTAL_TOKENS = 3;
    private final int FULL_BATCH_CALLS = 3;
    private final int PART_BATCH_CALLS = 2;
    private final int FULL_FACTORY_CALLS = 1;
    private final int PART_FACTORY_CALLS = 1;
    private final String IMS_HOST = "imstest.umiacs.umd.edu";

    private TokenCallback fullCallback;
    private TokenCallback partialCallback;

    // Autowired Beans
    @Autowired private BagRepository bagRepository;
    @Autowired private TokenRepository tokenRepository;

    // Two tokenizers for each bag we do
    private Tokenizer fullTokenizer;
    private Tokenizer partialTokenizer;

    // Our mocks which get injected
    @Mock private Tokenizer.IMSFactory factory = mock(Tokenizer.IMSFactory.class);
    @Mock private TokenRequestBatch batch = mock(TokenRequestBatch.class);

    @Before
    public void setup() {
        String fixityAlgorithm = "SHA-256";
        String stage = System.getProperty("chron.stage.bags");

        // Create the tokenizer for digesting all tokens
        Bag fullBag = bagRepository.findOne(Long.valueOf(1));
        Path fullPath = Paths.get(stage, fullBag.getBagStorage().getPath());
        fullCallback = new TokenCallback(tokenRepository, fullBag);
        fullTokenizer = new Tokenizer(fullPath, fixityAlgorithm, IMS_HOST, fullCallback, factory);

        // Create the tokenizer for digesting some tokens
        Bag partialBag = bagRepository.findOne(Long.valueOf(2));
        Path partialPath = Paths.get(stage, partialBag.getBagStorage().getPath());
        partialCallback = new TokenCallback(tokenRepository, partialBag);
        partialTokenizer = new Tokenizer(partialPath, fixityAlgorithm, IMS_HOST, partialCallback, factory);
    }


    @Test
    public void testFullTokenize() throws Exception {
        Filter<Path> filter = new TokenFilter(tokenRepository, 1L);
        when(factory.createIMSConnection(any(String.class), any(RequestBatchCallback.class)))
                .thenReturn(batch);

        doAnswer(invocation -> answer(invocation, fullCallback)).when(batch).add(any(TokenRequest.class));

        fullTokenizer.tokenize(filter);
        verifyMocks(FULL_FACTORY_CALLS, FULL_BATCH_CALLS, 1);
    }

    @Test
    public void testPartialTokenize() throws Exception {
        Filter<Path> filter = new TokenFilter(tokenRepository, 2L);
        when(factory.createIMSConnection(any(String.class), any(RequestBatchCallback.class)))
                .thenReturn(batch);
        doAnswer(invocation -> answer(invocation, partialCallback)).when(batch).add(any(TokenRequest.class));

        partialTokenizer.tokenize(filter);
        verifyMocks(PART_FACTORY_CALLS, PART_BATCH_CALLS, 2);
    }

    // helpers

    private void verifyMocks(int factoryTimes, int batchTimes, int bagId) throws InterruptedException {
        verify(factory, times(factoryTimes)).createIMSConnection(any(String.class), any(RequestBatchCallback.class));
        verify(batch, times(batchTimes)).add(any(TokenRequest.class));

        List<AceToken> tokens = tokenRepository.findByBagIdOrderByIdAsc(Long.valueOf(bagId));
        assertEquals(TOTAL_TOKENS, tokens.size());
    }

    private Object answer(InvocationOnMock invocation, TokenCallback callback) {
        // todo: look into whether we should return an actual object instead of null
        Object[] args = invocation.getArguments();
        TokenRequest arg = (TokenRequest) args[0];
        createResponse(arg, callback);
        return null;
    }

    private void createResponse(TokenRequest arg, TokenCallback callback) {
        // Build a token response
        TokenResponse response = new TokenResponse();
        response.setRoundId(1);
        response.setName(arg.getName());
        response.setDigestService("SHA-256");
        response.setStatusCode(StatusCode.SUCCESS);
        response.setDigestProvider("ims.umiacs.umd.edu");

        // Set the date
        GregorianCalendar calendar = new GregorianCalendar();
        response.setTimestamp(new XMLGregorianCalendarImpl(calendar));
        response.setTokenClassName("SHA-256");

        // create proof
        ProofElement element = new ProofElement();
        element.getHashes().add("test-token-" + response.getName());
        response.getProofElements().add(new ProofElement());

        callback.tokensReceived(
                ImmutableList.of(arg),
                ImmutableList.of(response)
        );
    }

}