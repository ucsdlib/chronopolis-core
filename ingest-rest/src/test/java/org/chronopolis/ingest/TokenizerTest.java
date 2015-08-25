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
import org.chronopolis.rest.models.AceToken;
import org.chronopolis.rest.models.Bag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.GregorianCalendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by shake on 8/25/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createBagsWithTokens.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sql/deleteBagsWithTokens.sql")
})
public class TokenizerTest extends IngestTest {
    private final int TOTAL_TOKENS = 3;
    private final int FULL_BATCH_CALLS = 3;
    private final int PART_BATCH_CALLS = 2;
    private final int FULL_FACTORY_CALLS = 1;
    private final int PART_FACTORY_CALLS = 1;

    TokenCallback fullCallback;
    TokenCallback partialCallback;

    @Autowired
    IngestSettings settings;

    @Autowired
    BagRepository bagRepository;

    @Autowired
    TokenRepository tokenRepository;

    // Two tokenizers for each bag we do
    @InjectMocks
    Tokenizer fullTokenizer;

    @InjectMocks
    Tokenizer partialTokenizer;

    @Mock
    Tokenizer.IMSFactory factory;

    @Mock
    TokenRequestBatch batch;

    @Before
    public void setup() {
        // Create the tokenizer for digesting all tokens
        Bag fullBag = bagRepository.findOne(Long.valueOf(1));
        Path fullPath = Paths.get(settings.getBagStage(), fullBag.getLocation());
        fullCallback = new TokenCallback(tokenRepository, fullBag);
        fullTokenizer = new Tokenizer(fullPath, fullBag.getFixityAlgorithm(), fullCallback);

        // Create the tokenizer for digesting some tokens
        Bag partialBag = bagRepository.findOne(Long.valueOf(2));
        Path partialPath = Paths.get(settings.getBagStage(), partialBag.getLocation());
        partialCallback = new TokenCallback(tokenRepository, partialBag);
        partialTokenizer = new Tokenizer(partialPath, partialBag.getFixityAlgorithm(), partialCallback);

        MockitoAnnotations.initMocks(this);
    }


    @Test
    public void testFullTokenize() throws Exception {
        Filter<Path> filter = new TokenFilter(tokenRepository, Long.valueOf(1));
        when(factory.createIMSConnection(any(RequestBatchCallback.class)))
                .thenReturn(batch);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                TokenRequest arg = (TokenRequest) args[0];
                createResponse(arg, fullCallback);
                return null;
            }
        }).when(batch).add(any(TokenRequest.class));

        fullTokenizer.tokenize(filter);

        verifyMocks(FULL_FACTORY_CALLS, FULL_BATCH_CALLS, 1);
    }

    @Test
    public void testPartialTokenize() throws Exception {
        Filter<Path> filter = new TokenFilter(tokenRepository, Long.valueOf(2));
        when(factory.createIMSConnection(any(RequestBatchCallback.class)))
                .thenReturn(batch);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                TokenRequest arg = (TokenRequest) args[0];
                createResponse(arg, partialCallback);
                return null;
            }
        }).when(batch).add(any(TokenRequest.class));

        partialTokenizer.tokenize(filter);

        verifyMocks(PART_FACTORY_CALLS, PART_BATCH_CALLS, 2);
    }

    // helpers

    void verifyMocks(int factoryTimes, int batchTimes, int bagId) throws InterruptedException {
        verify(factory, times(factoryTimes)).createIMSConnection(any(RequestBatchCallback.class));
        verify(batch, times(batchTimes)).add(any(TokenRequest.class));

        List<AceToken> tokens = tokenRepository.findByBagID(Long.valueOf(bagId));
        assertEquals(TOTAL_TOKENS, tokens.size());
    }

    void createResponse(TokenRequest arg, TokenCallback callback) {
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