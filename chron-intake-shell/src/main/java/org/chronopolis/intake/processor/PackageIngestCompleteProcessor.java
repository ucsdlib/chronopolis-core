/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.intake.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.pkg.PackageIngestCompleteMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.chronopolis.messaging.MessageConstant.STATUS_SUCCESS;

/**
 *
 * @author shake
 */
public class PackageIngestCompleteProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(PackageIngestCompleteProcessor.class);

    private ChronProducer producer;

    public PackageIngestCompleteProcessor(ChronProducer producer) {
        this.producer = producer;
    }

    @Override
    public void process(ChronMessage2 chronMessage) {
        if ( !(chronMessage instanceof PackageIngestCompleteMessage) ) {

        }

        PackageIngestCompleteMessage msg = (PackageIngestCompleteMessage) chronMessage;

        if ( msg.getStatus().equals(STATUS_SUCCESS.toString())) {
            log.info("Completed package ingestion of {} for correlation thread {}",
                    msg.getPackageName(), msg.getCorrelationId());
        } else {
            log.info("Could not ingest package {}", msg.getPackageName());
            for ( String item : msg.getFailedItems()) {
                log.info("{} failed", item);
            }
        }
        
    }

    
}
