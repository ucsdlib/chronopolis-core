package org.chronopolis.tokenize.batch;

import com.google.common.collect.ImmutableList;
import edu.umiacs.ace.ims.api.IMSException;
import edu.umiacs.ace.ims.ws.TokenRequest;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.rest.api.TokenService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.tokenize.ManifestEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChronopolisTokenRequestBatchTest {

    private Bag bag;
    private AceConfiguration configuration;
    private ChronopolisTokenRequestBatch batcher;

    @Mock
    private TokenService tokens;
    @Mock
    private ChronopolisTokenRequestBatch.ImsServiceWrapper wrapper;

    @Before
    public void setup() {
        bag = new Bag().setId(1L)
                .setName("test-name")
                .setDepositor("test-depositor");

        tokens = mock(TokenService.class);
        wrapper = mock(ChronopolisTokenRequestBatch.ImsServiceWrapper.class);
        configuration = new AceConfiguration()
                .setIms(new AceConfiguration.Ims().setEndpoint("test-ims-endpoint"));
    }

    // Various add tests

    @Test
    public void addBeyondQueueLength() throws Exception {
        batcher = new ChronopolisTokenRequestBatch(configuration, tokens);

        int bound = configuration.getIms().getQueueLength() * 2;
        for (int i = 0; i < bound; i++) {
            boolean add = batcher.add(new ManifestEntry(bag, "data/" + i, "test-digest"));
            Assert.assertTrue(add);
        }
        batcher.close();
    }

    @Test
    public void addWhenShutdown() {
        batcher = new ChronopolisTokenRequestBatch(configuration, tokens);
        batcher.close();

        Assert.assertFalse(batcher.add(new ManifestEntry(bag, "data", "test-digest")));
    }

    @Test
    public void addDuplicate() {
        batcher = new ChronopolisTokenRequestBatch(configuration, tokens);

        Assert.assertTrue(batcher.add(new ManifestEntry(bag, "data", "test-digest")));
        Assert.assertFalse(batcher.add(new ManifestEntry(bag, "data", "test-digest")));
    }

    @Test(expected = RuntimeException.class)
    public void addTokenRequest() throws Exception {
        batcher = new ChronopolisTokenRequestBatch(configuration, tokens);
        batcher.add(new TokenRequest());
    }

    @Test
    public void run() throws InterruptedException, ExecutionException {
        ExecutorService es = Executors.newFixedThreadPool(1);

        // http calls
        // we should aim for this to actually return a list of Responses to "process"
        when(wrapper.configuration()).thenReturn(configuration.getIms());
        when(wrapper.requestTokensImmediate(anyString(), anyList())).thenReturn(ImmutableList.of());

        batcher = new ChronopolisTokenRequestBatch(configuration, tokens, wrapper);
        es.submit(batcher);

        int bound = configuration.getIms().getQueueLength() * 2;
        for (int i = 0; i < bound; i++) {
            boolean add = batcher.add(new ManifestEntry(bag, "data/" + i, "test-digest"));
            Assert.assertTrue(add);
        }

        // allow the processing to happen
        TimeUnit.MILLISECONDS.sleep(100);
        verify(wrapper, times(2)).requestTokensImmediate(anyString(), anyList());
        batcher.close();
        es.shutdown();
    }

    @Test
    public void runImsException() throws InterruptedException {
        // most of this is shared, can break some of it apart easily
        ExecutorService es = Executors.newFixedThreadPool(1);
        when(wrapper.configuration()).thenReturn(configuration.getIms());
        when(wrapper.requestTokensImmediate(anyString(), anyList())).thenThrow(new IMSException(-1, "Test IMSException: Cannot connect to ims"));
        batcher = new ChronopolisTokenRequestBatch(configuration, tokens, wrapper);
        es.submit(batcher);
        int bound = configuration.getIms().getQueueLength();
        for (int i = 0; i < bound; i++) {
            boolean add = batcher.add(new ManifestEntry(bag, "data/" + i, "test-digest"));
            Assert.assertTrue(add);
        }

        TimeUnit.MILLISECONDS.sleep(100);
        batcher.close();
        verify(wrapper, times(1)).requestTokensImmediate(anyString(), anyList());
        Assert.assertEquals(0, batcher.getEntries().size());
        batcher.close();
        es.shutdown();
    }

}