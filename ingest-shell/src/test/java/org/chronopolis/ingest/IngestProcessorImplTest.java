/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest;

import org.chronopolis.common.properties.GenericProperties;
import org.chronopolis.ingest.processor.CollectionInitCompleteProcessor;
import org.chronopolis.ingest.processor.CollectionInitReplyProcessor;
import org.chronopolis.ingest.processor.CollectionRestoreCompleteProcessor;
import org.chronopolis.ingest.processor.CollectionRestoreReplyProcessor;
import org.chronopolis.ingest.processor.CollectionRestoreRequestProcessor;
import org.chronopolis.ingest.processor.PackageIngestStatusQueryProcessor;
import org.chronopolis.ingest.processor.PackageReadyProcessor;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.factory.MessageFactory;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author shake
 */
public class IngestProcessorImplTest {

    private IngestMessageListener listener;
    private GenericProperties properties;
    private MessageFactory messageFactory;
    private CollectionInitCompleteProcessor cicProcessor;
    private CollectionInitReplyProcessor cirProcessor;
    private CollectionRestoreRequestProcessor crrProcessor;
    private CollectionRestoreReplyProcessor crryProcessor;
    private CollectionRestoreCompleteProcessor crcProcessor;
    private PackageIngestStatusQueryProcessor pisqProcessor;
    private PackageReadyProcessor prProcessor; 

    @Before
    public void setUp() {
        prProcessor = EasyMock.createMock(PackageReadyProcessor.class);
        pisqProcessor = EasyMock.createMock(PackageIngestStatusQueryProcessor.class);
        cicProcessor = EasyMock.createMock(CollectionInitCompleteProcessor.class);
        cirProcessor = EasyMock.createMock(CollectionInitReplyProcessor.class);
        crrProcessor = EasyMock.createMock(CollectionRestoreRequestProcessor.class);
        crryProcessor = EasyMock.createMock(CollectionRestoreReplyProcessor.class);
        crcProcessor = EasyMock.createMock(CollectionRestoreCompleteProcessor.class);

        listener = new IngestMessageListener(pisqProcessor,
                prProcessor,
                cicProcessor,
                cirProcessor,
                crrProcessor,
                crryProcessor,
                crcProcessor);
        properties = new GenericProperties("node", "stage", "exchange", "inbound", "broadcast");
        messageFactory = new MessageFactory(properties);
    }
    
    @Test
    public void testListener() throws Exception {
        ChronMessage msg = messageFactory.defaultCollectionInitCompleteMessage();

        ChronProcessor p = listener.getProcessor(msg.getType());
        p.process(msg);
    }
}
