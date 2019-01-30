package org.chronopolis.tokenize.batch;

import com.google.common.collect.ImmutableSet;
import edu.umiacs.ace.ims.api.IMSException;
import edu.umiacs.ace.ims.ws.TokenRequest;
import edu.umiacs.ace.ims.ws.TokenResponse;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.tokenize.ManifestEntry;
import org.chronopolis.tokenize.supervisor.TokenWorkSupervisor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChronopolisTokenRequestBatchTest {

    private ChronopolisTokenRequestBatch batch;

    private final Set<ManifestEntry> manifestEntries = new HashSet<>();
    private final List<TokenResponse> tokenResponses = new ArrayList<>();
    private final ExecutorService es = Executors.newFixedThreadPool(1);

    @Mock private ImsServiceWrapper ims;
    @Mock private TokenWorkSupervisor supervisor;

    @Before
    public void setup() {
        Bag bag = new Bag(1L, 1L, 1L, null, null, ZonedDateTime.now(), ZonedDateTime.now(),
                "test-name", "ctrb-test", "test-depositor", BagStatus.DEPOSITED, new HashSet<>());

        // Setup the ManifestEntries and TokenResponses which will be used during processing
        for (int i = 0; i < 10; i++) {
            ManifestEntry entry = new ManifestEntry(bag, "data/path-" + i, "registered-digest");

            TokenRequest request = new TokenRequest();
            request.setName(entry.tokenName());
            request.setHashValue(entry.getDigest());

            TokenResponse response = new TokenResponse();
            response.setName(entry.tokenName());

            manifestEntries.add(entry);
            tokenResponses.add(response);
        }

        ims = mock(ImsServiceWrapper.class);
        supervisor = mock(TokenWorkSupervisor.class);
        AceConfiguration configuration = new AceConfiguration()
                .setIms(new AceConfiguration.Ims().setEndpoint("test-ims-endpoint"));

        batch = new ChronopolisTokenRequestBatch(configuration, ims, supervisor);
    }

    // Various add tests

    @Test
    public void whenShutdownNoProcessing() throws InterruptedException {
        batch.close();
        es.submit(batch);
        es.awaitTermination(0, TimeUnit.MILLISECONDS);
        batch.process(manifestEntries);

        verify(supervisor, times(0)).queuedEntries(anyInt(), anyLong(), any(TimeUnit.class));
        verify(supervisor, times(0)).associate(any(ManifestEntry.class), any(TokenResponse.class));
    }

    @Test
    public void processRequests() {
        when(ims.requestTokensImmediate(anyString(), anyList()))
                .thenReturn(tokenResponses);

        batch.process(manifestEntries);

        verify(supervisor, never()).queuedEntries(anyInt(), anyLong(), any(TimeUnit.class));
        verify(ims, atLeastOnce())
                .requestTokensImmediate(anyString(), anyList());
        verify(supervisor, times(10))
                .associate(any(ManifestEntry.class), any(TokenResponse.class));
    }

    @Test
    public void emptySetNotProcessed() {
        batch.process(ImmutableSet.of());

        verify(ims, never()).requestTokensImmediate(anyString(), anyList());
        verify(supervisor, never()).associate(any(ManifestEntry.class), any(TokenResponse.class));
        verify(supervisor, never()).retryTokenize(any(ManifestEntry.class));
    }

    @Test
    public void runImsException() {
        // most of this is shared, can break some of it apart easily
        when(ims.requestTokensImmediate(anyString(), anyList())).thenThrow(
                new IMSException(-1, "Test IMSException: Cannot connect to ims"));

        batch.process(manifestEntries);

        verify(ims, times(1)).requestTokensImmediate(anyString(), anyList());
        verify(supervisor, never()).associate(any(ManifestEntry.class), any(TokenResponse.class));
        verify(supervisor, times(10)).retryTokenize(any(ManifestEntry.class));
    }

}