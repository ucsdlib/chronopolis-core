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
 *
 * @author shake
 */
public class CollectionInitMessage extends ChronMessage2 {
    private final MessageType type = MessageType.DISTRIBUTE_COLL_INIT; 
    private static final String DEPOSIT_KEY = "depositor";
    private static final String COLLECTION_KEY = "collection";
    private static final String TOKENSTORE_KEY = "token-store";
    private static final String PERIOD_KEY = "audit-period";

    public CollectionInitMessage() {
        this.body = new ChronBody(type);
        this.header = new ChronHeader();
    }

    public CollectionInitMessage(MessageType type, ChronHeader header, ChronBody body) {
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

    @Override
    public void processMessage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
