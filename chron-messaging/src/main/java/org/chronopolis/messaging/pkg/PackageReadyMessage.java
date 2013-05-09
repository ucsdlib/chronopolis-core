/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.pkg;

import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronHeader;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.transfer.FileTransfer;
import org.chronopolis.transfer.RSyncTransfer;
import org.chronopolis.transfer.HttpsTransfer;

/**
 * Relay the state of the collection
 *
 * @author shake
 */
public class PackageReadyMessage extends ChronMessage2 {
    private final String NAME_KEY = "package-name";
    private final String LOCATION_KEY = "location";
    private final String DEPOSITOR_KEY = "depositor";
    private final String SIZE_KEY = "size";
    private final String PROTOCOL_KEY = "protocol";

    public PackageReadyMessage() {
        super(MessageType.PACKAGE_INGEST_READY);
        this.body = new ChronBody(type);
        this.header = new ChronHeader();
    }

    private String getProtocol() {
        return (String)body.get(PROTOCOL_KEY);
    }

    public String getPackageName() {
        return (String)body.get(NAME_KEY);
    }

    public String getDepositor() {
        return (String)body.get(DEPOSITOR_KEY);
    }

    public String getLocation() {
        return (String)body.get(LOCATION_KEY);
    }

    @Override
    public void processMessage() {
        // Things to do: 
        // 1: Validate message
        // 2: Grab bag
        // 3: validate and create token store
        
        String protocol = getProtocol();
        FileTransfer transferObj = null;

        if (protocol.equals("rsync")) {
            transferObj = new RSyncTransfer();
        } else if (protocol.equals("https")) {
            transferObj = new HttpsTransfer();
        } else {
            // Unsupported protocol
        }

        // Should end up being the location for a download
        String tokenStore = "https://chron-monitor.umiacs.umd.edu/tokenStore001";

        // Sending the next message will be done in the ingest consumer?
        CollectionInitMessage collectionInitRequest = new CollectionInitMessage(); 
        collectionInitRequest.setAuditPeriod("somedefinedperiod");
        collectionInitRequest.setCollection(getPackageName());
        collectionInitRequest.setDepositor(getDepositor());
        collectionInitRequest.setTokenStore(tokenStore);
        
        // Send message
    }
    
}
