package org.chronopolis.intake.processor;

import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.pkg.PackageReadyReplyMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shake on 2/3/14.
 */
public class PackageReadyReplyProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(PackageReadyReplyProcessor.class);

    @Override
    public void process(ChronMessage chronMessage) {
        if (!(chronMessage instanceof PackageReadyReplyMessage)) {
            //error
            log.error("Invalid message type: {}", chronMessage.getType());
        }
        PackageReadyReplyMessage msg = (PackageReadyReplyMessage) chronMessage;

        //check for ack/nak
        String att = msg.getMessageAtt();
        log.info("Received PackageReadyReply with Attribute {}", att);

    }

}
