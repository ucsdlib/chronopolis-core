/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.pkg;

import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronMessage2;

/**
 *
 * @author shake
 */
public class PackageIngestStatusQuery extends ChronMessage2 {

    public PackageIngestStatusQuery() {
        super(MessageType.PACKAGE_INGEST_STATUS_QUERY);
    }

    @Override
    public void processMessage() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
