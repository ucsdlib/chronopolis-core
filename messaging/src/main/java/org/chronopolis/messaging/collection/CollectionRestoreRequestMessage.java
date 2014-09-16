package org.chronopolis.messaging.collection;

import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronMessage;

import static org.chronopolis.messaging.MessageConstant.DEPOSITOR;
import static org.chronopolis.messaging.MessageConstant.COLLECTION;
import static org.chronopolis.messaging.MessageConstant.LOCATION;

/**
 * Created by shake on 7/10/14.
 */
public class CollectionRestoreRequestMessage extends ChronMessage {

    public CollectionRestoreRequestMessage() {
        super(MessageType.COLLECTION_RESTORE_REQUEST);
    }

    public void setCollection(String collection) {
        body.addContent(COLLECTION.toString(), collection);
    }

    public String getCollection() {
        return (String) body.get(COLLECTION.toString());
    }

    public void setDepositor(String depositor) {
        body.addContent(DEPOSITOR.toString(), depositor);
    }

    public String getDepositor() {
        return (String) body.get(DEPOSITOR.toString());
    }

    public void setLocation(String location) {
        body.addContent(LOCATION.toString(), location);
    }

    public String getLocation() {
        return (String) body.get(LOCATION.toString());
    }

}
