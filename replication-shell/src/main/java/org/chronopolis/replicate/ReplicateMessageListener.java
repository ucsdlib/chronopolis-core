/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate;

import org.apache.log4j.Logger;
import org.chronopolis.amqp.ChronMessageListener;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronProcessor;

/**
 *
 * @author shake
 */
public class ReplicateMessageListener extends ChronMessageListener {
    private static final Logger log = Logger.getLogger(ReplicateMessageListener.class);
    private ChronProcessor fileQueryProcessor;
    private ChronProcessor fileQueryResponseProcessor;
    private ChronProcessor collectionInitProcessor;

    public ReplicateMessageListener(ChronProcessor fileQueryProcessor,
                                    ChronProcessor fileQueryResponseProcessor,
                                    ChronProcessor collectionInitProcessor) {
        System.out.println("Hello Spring Context World!");
        log.info("Hello Spring Context World!");
        this.fileQueryProcessor = fileQueryProcessor;
        this.fileQueryResponseProcessor = fileQueryResponseProcessor;
        this.collectionInitProcessor = collectionInitProcessor;
    }

    @Override
    public ChronProcessor getProcessor(MessageType type) {
        switch (type) {
            case COLLECTION_INIT:
                return collectionInitProcessor;
            case FILE_QUERY:
                return fileQueryProcessor;
            case FILE_QUERY_RESPONSE:
                return fileQueryResponseProcessor;

            default:
                throw new RuntimeException("Unexpected MessageType: " + type.name());
        }
    }
    
}
