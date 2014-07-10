package org.chronopolis.messaging.collection;

import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronMessage;

/**
 * Created by shake on 7/10/14.
 */
public class CollectionRestoreReplyMessage extends ChronMessage {

    public CollectionRestoreReplyMessage() {
        super(MessageType.COLLECTION_RESTORE_REPLY);
    }

}
