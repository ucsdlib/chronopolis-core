package org.chronopolis.messaging.collection;

import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage;

import static org.chronopolis.messaging.MessageConstant.MESSAGE_ATT;

/**
 * Created by shake on 1/31/14.
 */
public class CollectionInitReplyMessage extends ChronMessage {

    public CollectionInitReplyMessage() {
        super(MessageType.COLLECTION_INIT_REPLY);
        this.body = new ChronBody(type);
    }

    public String getMessatAtt() {
        return (String) body.get(MESSAGE_ATT.toString());
    }

    public void setMessageAtt(Indicator ind) {
       body.addContent(MESSAGE_ATT.toString(), ind.getName());
    }
}
