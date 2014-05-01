/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.pkg.PackageIngestStatusQueryMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shake
 */
public class PackageIngestStatusQueryProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(PackageIngestStatusQueryProcessor.class);

    private final ChronProducer producer;

    public PackageIngestStatusQueryProcessor(ChronProducer producer) {
        this.producer = producer;
    }
    
    @Override
    public void process(ChronMessage chronMessage) {
        if ( !(chronMessage instanceof PackageIngestStatusQueryMessage) ) {
            // Error out
        }

        // Will need to make a query object for the replication services if we want this
    }

}
