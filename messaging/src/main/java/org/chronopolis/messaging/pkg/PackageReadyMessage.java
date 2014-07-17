/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.pkg;

import org.chronopolis.common.digest.Digest;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage;

import static org.chronopolis.messaging.MessageConstant.*;

/**
 * Message for packages which are ready to be ingested into
 * Chronopolis
 *
 * TODO: Size should be a Long
 *
 * @author shake
 */
public class PackageReadyMessage extends ChronMessage {

    public PackageReadyMessage() {
        super(MessageType.PACKAGE_INGEST_READY);
        this.body = new ChronBody(type);
    }

    public void setPackageName(String packageName) {
        body.addContent(PACKAGE_NAME.toString(), packageName);
    }

    public String getPackageName() {
        return (String) body.get(PACKAGE_NAME.toString());
    }

    public void setDepositor(String depositor) {
        body.addContent(DEPOSITOR.toString(), depositor);
    }

    public String getDepositor() {
        return (String) body.get(DEPOSITOR.toString());
    }

    public void setLocation(String location) {
        body.addContent(LOCATION.toString(), location);
    }

    public String getLocation() {
        return (String) body.get(LOCATION.toString());
    }

    public void setSize(int size) {
        body.addContent(SIZE.toString(), size);
    }

    public long getSize() {
        return (int) body.get(SIZE.toString());
    }

    public String getFixityAlgorithm() {
        return (String) body.get(FIXITY_ALGORITHM.toString());
    }

    public void setFixityAlgorithm(Digest algorithm) {
        body.addContent(FIXITY_ALGORITHM.toString(), algorithm.getName());
    }

}
