package org.chronopolis.ingest.processor;

import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionRestoreCompleteMessage;

/**
 * Created by shake on 8/8/14.
 */
public class CollectionRestoreCompleteProcessor implements ChronProcessor {
    @Override
    public void process(final ChronMessage chronMessage) {
        if (!(chronMessage instanceof CollectionRestoreCompleteMessage)) {
            throw new RuntimeException("Invalid message for "
                    + this.getClass().getName()
                    + ": "
                    + chronMessage.getClass().getName());
        }

        CollectionRestoreCompleteMessage msg =
                (CollectionRestoreCompleteMessage) chronMessage;
    }
}
