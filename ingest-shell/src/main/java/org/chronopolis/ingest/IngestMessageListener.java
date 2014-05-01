/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest;

import org.chronopolis.amqp.ChronMessageListener;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronProcessor;

/**
 * We could potentially change the constructors of the message listeners to be more like
 * xMessageListener(AProcessor aProccessor, BProcessor bProcessor)
 * and it wouldn't really change much, just enforce the type of the processors instead
 * of having any potential ChronProcessor
 *
 * @author shake 
 */
public class IngestMessageListener extends ChronMessageListener {
	private final ChronProcessor packageIngestStatusQueryProcessor;
	private final ChronProcessor packageReadyProcessor;
	private final ChronProcessor collectionInitCompleteProcessor;

	public IngestMessageListener(ChronProcessor packageIngestStatusQueryProcessor,
								 ChronProcessor packageReadyProcessor,
                                 ChronProcessor collectionInitCompleteProcessor) {
		this.packageIngestStatusQueryProcessor = packageIngestStatusQueryProcessor;
		this.packageReadyProcessor = packageReadyProcessor;
        this.collectionInitCompleteProcessor = collectionInitCompleteProcessor;
	}

	@Override
	public ChronProcessor getProcessor(MessageType type) {
        switch (type) {
            case PACKAGE_INGEST_READY:
                return packageReadyProcessor;
            case PACKAGE_INGEST_STATUS_QUERY:
                return packageIngestStatusQueryProcessor;
            case COLLECTION_INIT_COMPLETE:
                return collectionInitCompleteProcessor;
            default:
                throw new RuntimeException("Unexpected MessageType: " + type.name());
        }
	}
	
}
