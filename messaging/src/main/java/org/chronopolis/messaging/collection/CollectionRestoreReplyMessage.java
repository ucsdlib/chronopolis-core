package org.chronopolis.messaging.collection;

import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.MessageConstant;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronMessage;

/**
 * Created by shake on 8/7/14.
 */
public class CollectionRestoreReplyMessage extends ChronMessage {
    public CollectionRestoreReplyMessage() {
        super(MessageType.COLLECTION_RESTORE_REPLY);
    }

    public void setMessageAtt(Indicator att) {
        body.addContent(MessageConstant.MESSAGE_ATT.toString(), att.getName());
    }

    public String getMessageAtt() {
        return (String) body.get(MessageConstant.MESSAGE_ATT.toString());
    }

}
