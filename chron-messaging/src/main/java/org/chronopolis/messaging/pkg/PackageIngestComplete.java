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
 *
 * @author shake
 */
public class PackageIngestComplete extends ChronMessage2 {
    private final String STATUS_KEY = "status";
    private final String FAILED_KEY = "failed-items";

    public PackageIngestComplete() {
        super(MessageType.PACKAGE_INGEST_COMPLETE);
        this.body = new ChronBody(type);
        this.header = new ChronHeader();
    }

}
