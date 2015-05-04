/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate;

import org.chronopolis.amqp.ChronMessageListener;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.replicate.processor.CollectionRestoreLocationProcessor;
import org.chronopolis.replicate.processor.CollectionRestoreRequestProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Message listener for replication services. Delegate to a message processor
 * depending on the message received.
 *
 * @author shake
 */
@Deprecated
public class ReplicateMessageListener extends ChronMessageListener {
    private static final Logger log = LoggerFactory.getLogger(ReplicateMessageListener.class);
    private final ChronProcessor fileQueryProcessor;
    private final ChronProcessor fileQueryResponseProcessor;
    private final ChronProcessor collectionInitProcessor;
    private final CollectionRestoreRequestProcessor collectionRestoreRequestProcessor;
    private final CollectionRestoreLocationProcessor collectionRestoreLocationProcessor;

    public ReplicateMessageListener(ChronProcessor fileQueryProcessor,
                                    ChronProcessor fileQueryResponseProcessor,
                                    ChronProcessor collectionInitProcessor,
                                    CollectionRestoreRequestProcessor collectionRestoreRequestProcessor,
                                    CollectionRestoreLocationProcessor collectionRestoreLocationProcessor) {
        this.fileQueryProcessor = fileQueryProcessor;
        this.fileQueryResponseProcessor = fileQueryResponseProcessor;
        this.collectionInitProcessor = collectionInitProcessor;
        this.collectionRestoreRequestProcessor = collectionRestoreRequestProcessor;
        this.collectionRestoreLocationProcessor = collectionRestoreLocationProcessor;
    }

    /**
     * Retrieve the message processor for a given message
     *
     * @param type - The type of message which needs to be processed
     * @return - The {@Link ChronProcessor} for the message
     */
    @Override
    public ChronProcessor getProcessor(MessageType type) {
        switch (type) {
            case COLLECTION_INIT:
                return collectionInitProcessor;
            case FILE_QUERY:
                return fileQueryProcessor;
            case FILE_QUERY_RESPONSE:
                return fileQueryResponseProcessor;
            case COLLECTION_RESTORE_REQUEST:
                return collectionRestoreRequestProcessor;
            case COLLECTION_RESTORE_LOCATION:
                return collectionRestoreLocationProcessor;

            default:
                throw new RuntimeException("Unexpected MessageType: " + type.name());
        }
    }
    
}
