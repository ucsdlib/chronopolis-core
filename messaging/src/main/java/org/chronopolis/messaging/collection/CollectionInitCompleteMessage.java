/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.collection;

import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage;

/**
 * TODO: Do we want to convey some type of error, or is this only for notification of a success?
 *
 * @author shake
 */
public class CollectionInitCompleteMessage extends ChronMessage {

    public CollectionInitCompleteMessage() {
        super(MessageType.COLLECTION_INIT_COMPLETE);
        this.body = new ChronBody(type);
    }
    
}
