/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.collection;

import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronHeader;
import org.chronopolis.messaging.MessageType;


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
    private static final String DEPOSIT_KEY = "depositor";
    private static final String COLLECTION_KEY = "collection";
    private static final String TOKENSTORE_KEY = "token-store";
    private static final String PERIOD_KEY = "audit-period";

    public CollectionInitMessage() {
        super(MessageType.DISTRIBUTE_COLL_INIT);
        this.body = new ChronBody(type);
        this.header = new ChronHeader();
    }

    public CollectionInitMessage(ChronHeader header, ChronBody body) {
        super(MessageType.DISTRIBUTE_COLL_INIT);
        this.header = header;
        this.body = new ChronBody(type, body.getBody());
    }

    public void setDepositor(String depositor) {
        body.addContent(DEPOSIT_KEY, depositor);
    }

    public void setCollection(String collection) {
        body.addContent(COLLECTION_KEY, collection);
    }

    public void setTokenStore(String tokenStore) {
        body.addContent(TOKENSTORE_KEY, tokenStore);
    }

    public void setAuditPeriod(String auditPeriod) {
        body.addContent(PERIOD_KEY, auditPeriod);
    }

}
