package org.chronopolis.ingest.task;

import junit.framework.Assert;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.TestApplication;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.models.Bag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Test for the thread pool executor
 *
 * Created by shake on 5/22/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
public class TokenThreadPoolExecutorTest extends IngestTest {
    private final Logger log = LoggerFactory.getLogger(TokenThreadPoolExecutorTest.class);

    @Autowired
    BagRepository bags;

    @Autowired
    TokenRepository tokens;

    @Autowired
    IngestSettings settings;

    @InjectMocks
    TokenThreadPoolExecutor executor;

    @Mock
    private TokenRunner.Factory factory;

    Bag b = new Bag("test-name", "test-depositor");

    @Before
    public void setup() {
        executor = new TokenThreadPoolExecutor(4,
                4,
                4,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSubmitBagIfAvailable() throws Exception {
        when(factory.makeTokenRunner(
                any(Bag.class),
                any(String.class),
                any(String.class),
                any(BagRepository.class),
                any(TokenRepository.class)))
                .thenReturn(new SleepRunnable());

        boolean submitted;
        log.info("Submitting initial bag");
        submitted = executor.submitBagIfAvailable(b, settings, bags, tokens);
        Assert.assertEquals(submitted, true);

        for (int i = 0; i < 10; i++) {
            log.info("Submitting duplicate bag");
            submitted = executor.submitBagIfAvailable(b, settings, bags, tokens);
            Assert.assertEquals(submitted, false);
        }
    }

    private class SleepRunnable implements Runnable {
        @Override
        public void run() {
            try {
                // 2 seconds to have some buffer
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
            }
        }
    }

}