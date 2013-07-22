/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.chron.ingest.shell;

import org.chronopolis.ingest.IngestMessageListener;
import org.chronopolis.ingest.processor.CollectionInitCompleteProcessor;
import org.chronopolis.ingest.processor.PackageIngestStatusQueryProcessor;
import org.chronopolis.ingest.processor.PackageReadyProcessor;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitCompleteMessage;
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
    private CollectionInitCompleteProcessor cicProcessor; 
    private PackageIngestStatusQueryProcessor pisqProcessor; 
    private PackageReadyProcessor prProcessor; 

    @Before
    public void setUp() {
        prProcessor = EasyMock.createMock(PackageReadyProcessor.class);
        pisqProcessor = EasyMock.createMock(PackageIngestStatusQueryProcessor.class);
        cicProcessor = EasyMock.createMock(CollectionInitCompleteProcessor.class);

        listener = new IngestMessageListener(pisqProcessor, prProcessor, cicProcessor);
    }
    
    @Test
    public void testListener() throws Exception {
        ChronMessage2 msg = MessageFactory.DefaultCollectionInitCompleteMessage();

        ChronProcessor p = listener.getProcessor(msg.getType());
        p.process(msg);
    }
}
