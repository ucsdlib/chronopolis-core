package org.chronopolis.messaging.collection;

import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronMessage;

import static org.chronopolis.messaging.MessageConstant.LOCATION;
import static org.chronopolis.messaging.MessageConstant.MESSAGE_ATT;

/**
 * Created by shake on 7/10/14.
 */
public class CollectionRestoreCompleteMessage extends ChronMessage {

    public CollectionRestoreCompleteMessage() {
        super(MessageType.COLLECTION_RESTORE_COMPLETE);
    }

    public void setLocation(String location) {
        body.addContent(LOCATION.toString(), location);
    }

    public String getLocation() {
        return (String) body.get(LOCATION.toString());
    }

    public void setMessageAtt(Indicator messageAtt) {
        body.addContent(MESSAGE_ATT.toString(), messageAtt.getName());
    }

    public String getMessageAtt() {
        return (String) body.get(MESSAGE_ATT.toString());
    }

}
