/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.intake.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.messaging.pkg.PackageIngestCompleteMessage;

/**
 *
 * @author shake
 */
public class PackageIngestCompleteProcessor implements ChronProcessor {

    private ChronProducer producer;

    public PackageIngestCompleteProcessor(ChronProducer producer) {
        this.producer = producer;
    }

    @Override
    public void process(ChronMessage2 chronMessage) {
        if ( !(chronMessage instanceof PackageIngestCompleteMessage) ) {

        }

        ChronMessage2 msg = MessageFactory.DefaultPackageIngestCompleteMessage();
    }

    
}
