package org.chronopolis.intake.rest;

import org.chronopolis.amqp.ChronMessageListener;
import org.chronopolis.intake.processor.CollectionRestoreCompleteProcessor;
import org.chronopolis.intake.processor.PackageIngestCompleteProcessor;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronProcessor;

/**
 * Created by shake on 8/6/14.
 */
@Deprecated
public class DuracloudMessageListener extends ChronMessageListener {

    private final CollectionRestoreCompleteProcessor collectionRestoreCompleteProcessor;
    private final PackageIngestCompleteProcessor packageIngestCompleteProcessor;

    public DuracloudMessageListener(CollectionRestoreCompleteProcessor collectionRestoreCompleteProcessor,
                                    PackageIngestCompleteProcessor packageIngestCompleteProcessor) {
        this.collectionRestoreCompleteProcessor = collectionRestoreCompleteProcessor;
        this.packageIngestCompleteProcessor = packageIngestCompleteProcessor;
    }

    @Override
    public ChronProcessor getProcessor(final MessageType type) {
        switch (type) {
            case COLLECTION_RESTORE_COMPLETE:
                return collectionRestoreCompleteProcessor;
            case PACKAGE_INGEST_COMPLETE:
                return packageIngestCompleteProcessor;
            default:
                throw new RuntimeException("Unexpected message type! " + type);
        }
    }
}
