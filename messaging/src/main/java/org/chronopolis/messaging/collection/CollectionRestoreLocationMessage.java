package org.chronopolis.messaging.collection;

import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.MessageConstant;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronMessage;

/**
 * Created by shake on 8/7/14.
 */
public class CollectionRestoreLocationMessage extends ChronMessage {

    public CollectionRestoreLocationMessage() {
        super(MessageType.COLLECTION_RESTORE_LOCATION);
    }

    public void setRestoreLocation(String restoreLocation) {
        body.addContent(MessageConstant.RESTORE_LOCATION.toString(), restoreLocation);
    }

    public String getRestoreLocation() {
        return (String) body.get(MessageConstant.RESTORE_LOCATION.toString());
    }

    public void setProtocol(String protocol) {
        body.addContent(MessageConstant.PROTOCOL.toString(), protocol);
    }

    public String getProtocol() {
        return (String) body.get(MessageConstant.PROTOCOL.toString());
    }

    public void setMessageAtt(Indicator att) {
        body.addContent(MessageConstant.MESSAGE_ATT.toString(), att.getName());
    }

    public String getMessageAtt() {
        return (String) body.get(MessageConstant.MESSAGE_ATT.toString());
    }

}
