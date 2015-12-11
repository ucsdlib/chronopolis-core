/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.intake.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.exception.InvalidMessageException;
import org.chronopolis.messaging.pkg.PackageIngestCompleteMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.chronopolis.messaging.MessageConstant.STATUS_SUCCESS;

/**
 *
 * @author shake
 */
@Deprecated
public class PackageIngestCompleteProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(PackageIngestCompleteProcessor.class);

    private ChronProducer producer;

    public PackageIngestCompleteProcessor(ChronProducer producer) {
        this.producer = producer;
    }

    @Override
    public void process(ChronMessage chronMessage) {
        if (!(chronMessage instanceof PackageIngestCompleteMessage)) {
            throw new InvalidMessageException("Expected message of type PackageIngestComplete"
                   + " but received " + chronMessage.getClass().getName());
        }

        PackageIngestCompleteMessage msg = (PackageIngestCompleteMessage) chronMessage;

        if (msg.getStatus().equals(STATUS_SUCCESS.toString())) {
            log.info("Completed package ingestion of {} for correlation thread {}",
                    msg.getPackageName(), msg.getCorrelationId());
        } else {
            log.info("Could not ingest package {}", msg.getPackageName());
            for (String item : msg.getFailedItems()) {
                log.info("{} failed", item);
            }
        }

    }


}
