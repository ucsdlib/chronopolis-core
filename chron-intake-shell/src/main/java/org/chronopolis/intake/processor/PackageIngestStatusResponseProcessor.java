/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.intake.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.pkg.PackageIngestStatusResponseMessage;

/**
 *
 * @author shake
 */
public class PackageIngestStatusResponseProcessor implements ChronProcessor {

    private ChronProducer producer;

    public PackageIngestStatusResponseProcessor(ChronProducer producer) {
        this.producer = producer;
    }

    @Override
    public void process(ChronMessage2 chronMessage) {
        if ( !(chronMessage instanceof PackageIngestStatusResponseMessage)) {
            // Error out
        }
    }

}
