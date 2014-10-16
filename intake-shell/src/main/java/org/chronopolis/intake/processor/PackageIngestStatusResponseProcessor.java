/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.intake.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.pkg.PackageIngestStatusResponseMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shake
 */
public class PackageIngestStatusResponseProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(PackageIngestStatusResponseProcessor.class);

    private ChronProducer producer;

    public PackageIngestStatusResponseProcessor(ChronProducer producer) {
        this.producer = producer;
    }

    @Override
    public void process(ChronMessage chronMessage) {
        if (!(chronMessage instanceof PackageIngestStatusResponseMessage)) {
            // Error out
            log.error("Invalid message type: {}", chronMessage.getType());
        }
    }

}
