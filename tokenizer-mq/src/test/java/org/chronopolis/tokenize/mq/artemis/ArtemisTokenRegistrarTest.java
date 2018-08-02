package org.chronopolis.tokenize.mq.artemis;

import org.chronopolis.test.support.CallWrapper;
import org.junit.Test;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ArtemisTokenRegistrarTest extends MqTest {

    @Test
    public void run() throws Exception {
        sendRegisterMessage();
        when(tokens.createToken(eq(ID), any())).thenReturn(new CallWrapper<>(null));
        ArtemisTokenRegistrar registrar =
                new ArtemisTokenRegistrar(0, MILLISECONDS, tokens, serverLocator, mapper);
        registrar.run();
        verify(tokens, times(1)).createToken(eq(ID), any());
    }

}