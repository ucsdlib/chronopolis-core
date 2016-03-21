package org.chronopolis.ingest.task;

import com.google.common.base.Optional;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.TestApplication;
import org.chronopolis.ingest.TrackingThreadPoolExecutor;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.rest.entities.Bag;
import org.junit.Assert;
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

import java.lang.reflect.Field;
import java.util.concurrent.FutureTask;
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

    TrackingThreadPoolExecutor<Bag> trackingExecutor;

    @Mock
    private TokenRunner.Factory factory;

    Bag b0 = new Bag("test-name-0", "test-depositor");
    Bag b1 = new Bag("test-name-1", "test-depositor");

    @Before
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        executor = new TokenThreadPoolExecutor(4,
                4,
                4,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

        trackingExecutor = new TrackingThreadPoolExecutor<>(4,
                4,
                4,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>());

        MockitoAnnotations.initMocks(this);

        // ensure the ids are not null
        Field id = Bag.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(b1, 1L);
        id.set(b0, 0L);
    }

    //@Test
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
        submitted = executor.submitBagIfAvailable(b0, settings, bags, tokens);
        Assert.assertEquals(submitted, true);

        for (int i = 0; i < 10; i++) {
            log.info("Submitting duplicate bag");
            submitted = executor.submitBagIfAvailable(b0, settings, bags, tokens);
            Assert.assertEquals(submitted, false);
        }
    }

    @Test
    public void testTrackingPoolSubmitBag() throws Exception {
        Runnable r = new SleepRunnable();
        log.info("Submitting initial bag");
        Optional<FutureTask<Bag>> future = trackingExecutor.submitIfAvailable(r, b0);
        Assert.assertTrue(future.isPresent());
        for (int i = 0; i < 10; i++) {
            log.info("Submitting duplicate bag");
            future = trackingExecutor.submitIfAvailable(r, b0);
            Assert.assertFalse(future.isPresent());
        }
    }

    @Test
    public void testTrackingPoolExceptionRunnable() throws Exception {
        Runnable r = new ExceptionRunnable();
        log.info("Submitting exception");
        Optional<FutureTask<Bag>> future = trackingExecutor.submitIfAvailable(r, b1);
        Assert.assertTrue(future.isPresent());
        // give it time to execute
        TimeUnit.SECONDS.sleep(1);
        Assert.assertFalse(trackingExecutor.contains(b1));
    }

    private class ExceptionRunnable implements Runnable {
        @Override
        public void run() {
            log.info("Throwing exception");
            throw new RuntimeException("Test exception");
        }
    }

    private class SleepRunnable implements Runnable {
        @Override
        public void run() {
            try {
                // 2 seconds to have some buffer
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException ignored) {
            }
        }
    }

}