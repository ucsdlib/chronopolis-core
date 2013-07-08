/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.collection;

import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronHeader;
import org.chronopolis.messaging.MessageType;

import static org.chronopolis.messaging.MessageConstant.DEPOSITOR;
import static org.chronopolis.messaging.MessageConstant.COLLECTION;
import static org.chronopolis.messaging.MessageConstant.TOKEN_STORE;
import static org.chronopolis.messaging.MessageConstant.AUDIT_PERIOD;

/**
 * Message an Ingest Node users to tell a Replicating Node to initialize a new 
 * collection
 * 
 * Initializing will involve two parts for the Replicating Node:
 *    - Creating an ACE collection
 *    - Grabbing the data from the staging area
 * 
 * @author shake
 */
public class CollectionInitMessage extends ChronMessage2 {
    public CollectionInitMessage() {
        super(MessageType.COLLECTION_INIT);
        this.body = new ChronBody(type);
    }

    public CollectionInitMessage(ChronHeader header, ChronBody body) {
        super(MessageType.COLLECTION_INIT);
        this.body = new ChronBody(type, body.getBody());
    }

    public void setDepositor(String depositor) {
        body.addContent(DEPOSITOR.toString(), depositor);
    }

    public void setCollection(String collection) {
        body.addContent(COLLECTION.toString(), collection);
    }

    public void setTokenStore(String tokenStore) {
        body.addContent(TOKEN_STORE.toString(), tokenStore);
    }

    public void setAuditPeriod(String auditPeriod) {
        body.addContent(AUDIT_PERIOD.toString(), auditPeriod);
    }

}
