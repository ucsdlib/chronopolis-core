/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.collection;

import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage;

/**
 *
 * @author shake
 */
public class CollectionInitCompleteMessage extends ChronMessage {

    public CollectionInitCompleteMessage() {
        super(MessageType.COLLECTION_INIT_COMPLETE);
        this.body = new ChronBody(type);
    }
    
}
