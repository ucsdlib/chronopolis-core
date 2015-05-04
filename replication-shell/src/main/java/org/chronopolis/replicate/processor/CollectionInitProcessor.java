/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.processor;

import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.replicate.batch.ReplicationJobStarter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: How to reply to collection init message if there is an error
 *
 * @author shake
 */
@Deprecated
public class CollectionInitProcessor implements ChronProcessor {
    private static final Logger log = LoggerFactory.getLogger(CollectionInitProcessor.class);

    private ReplicationJobStarter replicationJobStarter;

    public CollectionInitProcessor(ReplicationJobStarter replicationJobStarter) {
        this.replicationJobStarter = replicationJobStarter;
    }


    @Override
    public void process(ChronMessage chronMessage) {
        if(!(chronMessage instanceof CollectionInitMessage)) {
            // Error out
            log.error("Incorrect Message Type");
            return;
        }

        log.trace("Received collection init message");

        CollectionInitMessage msg = (CollectionInitMessage) chronMessage;

        replicationJobStarter.addJobFromMessage(msg);
    }

}
