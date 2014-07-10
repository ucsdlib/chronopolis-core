package org.chronopolis.messaging.collection;

import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronMessage;

/**
 * Created by shake on 7/10/14.
 */
public class CollectionRestoreRequestMessage extends ChronMessage {

    public CollectionRestoreRequestMessage() {
        super(MessageType.COLLECTION_RESTORE_REQUEST);
    }

}
