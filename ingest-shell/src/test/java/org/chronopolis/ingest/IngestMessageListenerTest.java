package org.chronopolis.ingest;

import junit.framework.Assert;
import org.chronopolis.amqp.ChronMessageListener;
import org.chronopolis.common.properties.GenericProperties;
import org.chronopolis.ingest.processor.CollectionInitCompleteProcessor;
import org.chronopolis.ingest.processor.CollectionInitReplyProcessor;
import org.chronopolis.ingest.processor.CollectionRestoreCompleteProcessor;
import org.chronopolis.ingest.processor.CollectionRestoreRequestProcessor;
import org.chronopolis.ingest.processor.PackageIngestStatusQueryProcessor;
import org.chronopolis.ingest.processor.PackageReadyProcessor;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.factory.MessageFactory;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IngestMessageListenerTest {
    MessageFactory messageFactory;
    ChronMessageListener messageListener;
    ChronProcessor processor;
    PackageIngestStatusQueryProcessor pisqProcessor;
    CollectionInitReplyProcessor cirProcessor;
    CollectionRestoreRequestProcessor crrProcessor;
    CollectionRestoreCompleteProcessor crcProcessor;
    CollectionInitCompleteProcessor cicProcessor;
    PackageReadyProcessor prProcessor;

    @Before
    public void setup() {
        GenericProperties properties = new GenericProperties(
                "string",
                "string",
                "string",
                "string",
                "string"
        );

        messageFactory = new MessageFactory(properties);

        pisqProcessor = EasyMock.createMock(PackageIngestStatusQueryProcessor.class);
        prProcessor = EasyMock.createMock(PackageReadyProcessor.class);
        cicProcessor = EasyMock.createMock(CollectionInitCompleteProcessor.class);
        cirProcessor = EasyMock.createMock(CollectionInitReplyProcessor.class);
        crrProcessor = EasyMock.createMock(CollectionRestoreRequestProcessor.class);
        crcProcessor = EasyMock.createMock(CollectionRestoreCompleteProcessor.class);

        processor = EasyMock.createMock(ChronProcessor.class);
        messageListener = new IngestMessageListener(
                pisqProcessor,
                prProcessor,
                cicProcessor,
                cirProcessor,
                crrProcessor,
                crcProcessor);
    }

    @Test
    public void testGetProcessor() throws Exception {
        processor = messageListener.getProcessor(MessageType.PACKAGE_INGEST_READY);
        Assert.assertEquals(processor, prProcessor);
        processor = messageListener.getProcessor(MessageType.PACKAGE_INGEST_STATUS_QUERY);
        Assert.assertEquals(processor, pisqProcessor);
        processor = messageListener.getProcessor(MessageType.COLLECTION_INIT_COMPLETE);
        Assert.assertEquals(processor, cicProcessor);
        processor = messageListener.getProcessor(MessageType.COLLECTION_INIT_REPLY);
        Assert.assertEquals(processor, cirProcessor);
        processor = messageListener.getProcessor(MessageType.COLLECTION_RESTORE_REQUEST);
        Assert.assertEquals(processor, crrProcessor);
    }
}