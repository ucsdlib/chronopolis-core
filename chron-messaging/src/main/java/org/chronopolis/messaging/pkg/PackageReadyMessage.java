/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.pkg;

import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronHeader;
import org.chronopolis.messaging.base.ChronMessage2;

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

    public PackageReadyMessage() {
        super(MessageType.PACKAGE_INGEST_READY);
        this.body = new ChronBody(type);
        this.header = new ChronHeader();
    }

    @Override
    public void processMessage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
