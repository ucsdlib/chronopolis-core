package org.chronopolis.intake.processor;

import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.pkg.PackageReadyReplyMessage;

/**
 * Created by shake on 2/3/14.
 */
public class PackageReadyReplyProcessor implements ChronProcessor {

    @Override
    public void process(ChronMessage chronMessage) {
        if ( !(chronMessage instanceof PackageReadyReplyMessage)) {
            //error
        }
        PackageReadyReplyMessage msg = (PackageReadyReplyMessage) chronMessage;

        //check for ack/nak
        String att = msg.getMessageAtt();
        System.out.println(att);


    }
}
