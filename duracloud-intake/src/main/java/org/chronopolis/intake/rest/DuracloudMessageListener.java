package org.chronopolis.intake.rest;

import org.chronopolis.amqp.ChronMessageListener;
import org.chronopolis.intake.processor.CollectionRestoreReplyProcessor;
import org.chronopolis.intake.processor.PackageIngestCompleteProcessor;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronProcessor;

/**
 * Created by shake on 8/6/14.
 */
public class DuracloudMessageListener extends ChronMessageListener {

    private final CollectionRestoreReplyProcessor collectionRestoreReplyProcessor;
    private final PackageIngestCompleteProcessor packageIngestCompleteProcessor;

    public DuracloudMessageListener(CollectionRestoreReplyProcessor collectionRestoreReplyProcessor,
                                    PackageIngestCompleteProcessor packageIngestCompleteProcessor) {
        this.collectionRestoreReplyProcessor = collectionRestoreReplyProcessor;
        this.packageIngestCompleteProcessor = packageIngestCompleteProcessor;
    }

    @Override
    public ChronProcessor getProcessor(final MessageType type) {
        switch (type) {
            case COLLECTION_RESTORE_COMPLETE:
                return collectionRestoreReplyProcessor;
            case PACKAGE_INGEST_COMPLETE:
                return packageIngestCompleteProcessor;
            default:
                throw new RuntimeException("Unexpected message type! " + type);
        }
    }
}
