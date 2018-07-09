package org.chronopolis.tokenize.mq.artemis;

import org.chronopolis.rest.models.Bag;
import org.chronopolis.tokenize.ManifestEntry;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Brief tests for the ArtemisSupervisor
 * <p>
 * todo: could use a consumer to check that messages are actually sent
 *
 * @author shake
 */
public class ArtemisSupervisorTest extends MqTest {

    @Test
    public void start() throws Exception {
        ArtemisSupervisor supervisor = new ArtemisSupervisor(serverLocator, mapper, tokens, imsWrapper, executor, executor);

        when(executor.getActiveCount()).thenReturn(0);
        boolean start = supervisor.start(createEntry());
        verify(executor, times(5)).submit(any(Runnable.class));
        Assert.assertTrue(start);
    }

    @Test
    public void associate() throws Exception {
        ArtemisSupervisor supervisor = new ArtemisSupervisor(serverLocator, mapper, tokens, imsWrapper, executor, executor);

        when(executor.getActiveCount()).thenReturn(0);
        boolean associate = supervisor.associate(createEntry(), createTokenResponse());
        verify(executor, times(5)).submit(any(Runnable.class));
        Assert.assertTrue(associate);
    }

    @Test
    public void isProcessing() throws Exception {
        sendRequestMessage();

        when(executor.getActiveCount()).thenReturn(0);
        ArtemisSupervisor supervisor = new ArtemisSupervisor(serverLocator, mapper, tokens, imsWrapper, executor, executor);
        Assert.assertTrue(supervisor.isProcessing());
        verify(executor, times(5)).submit(any(Runnable.class));
    }

    @Test
    public void isProcessingEntry() throws Exception {
        sendRequestMessage();

        ManifestEntry entry = createEntry();
        when(executor.getActiveCount()).thenReturn(0);
        ArtemisSupervisor supervisor = new ArtemisSupervisor(serverLocator, mapper, tokens, imsWrapper, executor, executor);
        Assert.assertTrue(supervisor.isProcessing(entry));
    }

    @Test
    public void isProcessingBag() throws Exception {
        sendRequestMessage();

        Bag bag = createBag();
        when(executor.getActiveCount()).thenReturn(0);
        ArtemisSupervisor supervisor = new ArtemisSupervisor(serverLocator, mapper, tokens, imsWrapper, executor, executor);
        Assert.assertTrue(supervisor.isProcessing(bag));
    }

}