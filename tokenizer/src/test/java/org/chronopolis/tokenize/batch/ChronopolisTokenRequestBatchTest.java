package org.chronopolis.tokenize.batch;

import com.google.common.collect.ImmutableSet;
import edu.umiacs.ace.ims.api.IMSException;
import edu.umiacs.ace.ims.ws.TokenRequest;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.StateMachine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChronopolisTokenRequestBatchTest {

    private ChronopolisTokenRequestBatch batch;

    private final List<TokenRequest> tokenRequests = new ArrayList<>();
    private final Set<ManifestEntry> manifestEntries = new HashSet<>();
    private final List<TokenResponse> tokenResponses = new ArrayList<>();
    private final ExecutorService es = Executors.newFixedThreadPool(1);

    @Mock private ImsServiceWrapper ims;
    @Mock private StateMachine stateMachine;

    @Before
    public void setup() {
        Bag bag = new Bag().setId(1L)
                .setName("test-name")
                .setDepositor("test-depositor");

        // Setup the ManifestEntries and TokenResponses which will be used during processing
        for (int i = 0; i < 10; i++) {
            ManifestEntry entry = new ManifestEntry(bag, "data/path-" + i, "registered-digest");
            entry.setCalculatedDigest("registered-digest");

            TokenRequest request = new TokenRequest();
            request.setName(entry.tokenName());
            request.setHashValue(entry.getCalculatedDigest());

            TokenResponse response = new TokenResponse();
            response.setName(entry.getPath());

            manifestEntries.add(entry);
            tokenRequests.add(request);
            tokenResponses.add(response);
        }

        ims = mock(ImsServiceWrapper.class);
        stateMachine = mock(StateMachine.class);
        AceConfiguration configuration = new AceConfiguration()
                .setIms(new AceConfiguration.Ims().setEndpoint("test-ims-endpoint"));

        batch = new ChronopolisTokenRequestBatch(configuration, ims, stateMachine);
    }

    // Various add tests

    @Test
    public void whenShutdownNoProcessing() throws InterruptedException {
        batch.close();
        es.submit(batch);
        es.awaitTermination(10, TimeUnit.MILLISECONDS);
        batch.process(manifestEntries);

        verify(stateMachine, times(0)).queuedEntries(anyInt(), anyLong(), any(TimeUnit.class));
        verify(stateMachine, times(0)).associate(any(ManifestEntry.class), any(TokenResponse.class));
    }

    @Test
    public void runSuccess() throws InterruptedException {
        when(stateMachine.queuedEntries(anyInt(), anyLong(), any(TimeUnit.class)))
                .thenReturn(manifestEntries);

        when(ims.requestTokensImmediate(anyString(), anyListOf(TokenRequest.class)))
                .thenReturn(tokenResponses);

        es.submit(batch);
        TimeUnit.MILLISECONDS.sleep(50);
        batch.close();
        es.awaitTermination(500, TimeUnit.MILLISECONDS);

        verify(stateMachine, atLeastOnce()).queuedEntries(anyInt(), anyLong(), any(TimeUnit.class));
        verify(ims, atLeastOnce())
                .requestTokensImmediate(anyString(), anyListOf(TokenRequest.class));
        verify(stateMachine, atLeast(10))
                .associate(any(ManifestEntry.class), any(TokenResponse.class));
    }

    @Test
    public void emptySetNotProcessed() {
        batch.process(ImmutableSet.of());

        verify(ims, never()).requestTokensImmediate(anyString(), anyListOf(TokenRequest.class));
        verify(stateMachine, never()).associate(any(ManifestEntry.class), any(TokenResponse.class));
        verify(stateMachine, never()).retryTokenize(any(ManifestEntry.class));
    }

    @Test
    public void runImsException() {
        // most of this is shared, can break some of it apart easily
        when(ims.requestTokensImmediate(anyString(), anyListOf(TokenRequest.class))).thenThrow(
                new IMSException(-1, "Test IMSException: Cannot connect to ims"));

        batch.process(manifestEntries);

        verify(ims, times(1)).requestTokensImmediate(anyString(), anyListOf(TokenRequest.class));
        verify(stateMachine, never()).associate(any(ManifestEntry.class), any(TokenResponse.class));
        verify(stateMachine, times(10)).retryTokenize(any(ManifestEntry.class));
    }

}