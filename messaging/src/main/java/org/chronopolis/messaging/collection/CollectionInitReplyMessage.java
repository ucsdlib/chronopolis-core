package org.chronopolis.messaging.collection;

import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage;

import java.util.List;

import static org.chronopolis.messaging.MessageConstant.COLLECTION;
import static org.chronopolis.messaging.MessageConstant.DEPOSITOR;
import static org.chronopolis.messaging.MessageConstant.FAILED_ITEMS;
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

    public List<String> getFailedItems() {
        return (List<String>) body.get(FAILED_ITEMS.toString());
    }

    public void setFailedItems(List<String> failedItems) {
        body.addContent(FAILED_ITEMS.toString(), failedItems);
    }

    public String getDepositor() {
        return (String) body.get(DEPOSITOR.toString());
    }

    public void setDepositor(String depositor) {
        body.addContent(DEPOSITOR.toString(), depositor);
    }

    public String getCollection() {
        return (String) body.get(COLLECTION.toString());
    }

    public void setCollection(String collection) {
        body.addContent(COLLECTION.toString(), collection);
    }

}
