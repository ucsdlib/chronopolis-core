package org.chronopolis.intake;

import junit.framework.Assert;
import org.chronopolis.intake.processor.PackageIngestCompleteProcessor;
import org.chronopolis.intake.processor.PackageIngestStatusResponseProcessor;
import org.chronopolis.intake.processor.PackageReadyReplyProcessor;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronProcessor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Created by shake on 2/5/14.
 */
@RunWith(JUnit4.class)
public class IntakeMessageListenerTest {
    IntakeMessageListener listener;
    PackageIngestCompleteProcessor packageIngestCompleteProcessor;
    PackageIngestStatusResponseProcessor packageIngestStatusResponseProcessor;
    PackageReadyReplyProcessor packageReadyReplyProcessor;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        // These don't need a producer since we're only returning them
        packageIngestCompleteProcessor =
                new PackageIngestCompleteProcessor(null);
        packageIngestStatusResponseProcessor =
                new PackageIngestStatusResponseProcessor(null);
        packageReadyReplyProcessor = new PackageReadyReplyProcessor();


        listener = new IntakeMessageListener(
                packageIngestCompleteProcessor,
                packageIngestStatusResponseProcessor,
                packageReadyReplyProcessor
        );

    }

    @Test
    public void testGetProcessor() throws Exception {
        // We should get the same processor back, so check memory equality
        ChronProcessor processor = listener.getProcessor(MessageType.PACKAGE_INGEST_COMPLETE);
        Assert.assertTrue(processor == packageIngestCompleteProcessor);

        processor = listener.getProcessor(MessageType.PACKAGE_INGEST_STATUS_RESPONSE);
        Assert.assertTrue(processor == packageIngestStatusResponseProcessor);

        processor = listener.getProcessor(MessageType.PACKAGE_INGEST_READY_REPLY);
        Assert.assertTrue(processor == packageReadyReplyProcessor);

        // And a message we don't expect
        thrown.expect(RuntimeException.class);
        processor = listener.getProcessor(MessageType.COLLECTION_INIT);
    }
}
