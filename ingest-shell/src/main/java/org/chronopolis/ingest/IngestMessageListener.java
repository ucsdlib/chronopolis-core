/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest;

import org.chronopolis.amqp.ChronMessageListener;
import org.chronopolis.ingest.processor.CollectionInitCompleteProcessor;
import org.chronopolis.ingest.processor.CollectionInitReplyProcessor;
import org.chronopolis.ingest.processor.PackageIngestStatusQueryProcessor;
import org.chronopolis.ingest.processor.PackageReadyProcessor;
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
    private final PackageIngestStatusQueryProcessor packageIngestStatusQueryProcessor;
    private final PackageReadyProcessor packageReadyProcessor;
    private final CollectionInitCompleteProcessor collectionInitCompleteProcessor;
    private final CollectionInitReplyProcessor collectionInitReplyProcessor;

    public IngestMessageListener(PackageIngestStatusQueryProcessor packageIngestStatusQueryProcessor,
                                 PackageReadyProcessor packageReadyProcessor,
                                 CollectionInitCompleteProcessor collectionInitCompleteProcessor,
                                 CollectionInitReplyProcessor collectionInitReplyProcessor) {
        this.packageIngestStatusQueryProcessor = packageIngestStatusQueryProcessor;
        this.packageReadyProcessor = packageReadyProcessor;
        this.collectionInitCompleteProcessor = collectionInitCompleteProcessor;
        this.collectionInitReplyProcessor = collectionInitReplyProcessor;
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
            case COLLECTION_INIT_REPLY:
                return collectionInitReplyProcessor;
            default:
                throw new RuntimeException("Unexpected MessageType: " + type.name());
        }
    }

}
