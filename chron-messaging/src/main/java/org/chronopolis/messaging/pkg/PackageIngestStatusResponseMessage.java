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
public class PackageIngestStatusResponseMessage extends ChronMessage2 {
    public PackageIngestStatusResponseMessage() {
        super(MessageType.PACKAGE_INGEST_STATUS_RESPONSE);
    }

}
