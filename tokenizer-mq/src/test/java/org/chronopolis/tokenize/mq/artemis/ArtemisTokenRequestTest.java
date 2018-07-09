package org.chronopolis.tokenize.mq.artemis;

import com.google.common.collect.ImmutableList;
import org.chronopolis.common.ace.AceConfiguration;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArtemisTokenRequestTest extends MqTest {

    private ArtemisSupervisor supervisor = mock(ArtemisSupervisor.class);
    private AceConfiguration.Ims imsConfig = new AceConfiguration.Ims()
            .setWaitTime(5)
            .setQueueLength(1);

    @Test
    public void run() throws Exception {
        sendRequestMessage();

        when(imsWrapper.configuration()).thenReturn(imsConfig);
        when(imsWrapper.requestTokensImmediate(eq(imsConfig.getTokenClass()), any())).thenReturn(ImmutableList.of(createTokenResponse()));
        ArtemisTokenRequest request = new ArtemisTokenRequest(0, TimeUnit.MILLISECONDS, imsWrapper, supervisor, serverLocator, mapper);
        request.run();

        verify(imsWrapper, atLeast(3)).configuration();
        verify(imsWrapper, times(1)).requestTokensImmediate(eq(imsConfig.getTokenClass()), any());
        verify(supervisor, times(1)).associate(any(), any());
    }

}