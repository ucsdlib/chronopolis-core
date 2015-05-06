package org.chronopolis.intake.processor;

import org.chronopolis.messaging.MessageConstant;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.pkg.PackageIngestCompleteMessage;

/**
 * Created by shake on 7/17/14.
 */
@Deprecated
public class PackageIngestCompleteProcessor implements ChronProcessor {

    public PackageIngestCompleteProcessor() {
    }

    @Override
    public void process(final ChronMessage chronMessage) {
        if (!(chronMessage instanceof PackageIngestCompleteMessage)) {
            throw new RuntimeException("Invalid message type! " + chronMessage.getClass().getName());
        }

        PackageIngestCompleteMessage message = (PackageIngestCompleteMessage) chronMessage;

        if (MessageConstant.STATUS_SUCCESS.equals(message.getStatus())) {
            // update BagStatus
            // statusAccessor.get(message.getPackageName())
        }

        message.getStatus();
        message.getPackageName();

        // TODO: Notify duracloud of successful ingest

    }

}
