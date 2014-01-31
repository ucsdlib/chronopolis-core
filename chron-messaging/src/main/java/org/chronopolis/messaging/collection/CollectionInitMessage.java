/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.collection;

import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage;
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
public class CollectionInitMessage extends ChronMessage {
    public CollectionInitMessage() {
        super(MessageType.COLLECTION_INIT);
        this.body = new ChronBody(type);
    }

    public void setDepositor(String depositor) {
        body.addContent(DEPOSITOR.toString(), depositor);
    }

    public String getDepositor() {
        return (String) body.get(DEPOSITOR.toString());
    }

    public void setCollection(String collection) {
        body.addContent(COLLECTION.toString(), collection);
    }

    public String getCollection() {
        return (String) body.get(COLLECTION.toString());
    }

    public void setTokenStore(String tokenStore) {
        body.addContent(TOKEN_STORE.toString(), tokenStore);
    }

    public String getTokenStore() {
        return (String) body.get(TOKEN_STORE.toString());

    }

    public void setAuditPeriod(long auditPeriod) {
        body.addContent(AUDIT_PERIOD.toString(), auditPeriod);
    }

    public long getAuditPeriod() {
        return (long) body.get(AUDIT_PERIOD.toString());
    }

}
