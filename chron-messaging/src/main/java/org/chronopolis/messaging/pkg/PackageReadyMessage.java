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

    /*
    public void setProtocol(String protocol) {
        body.addContent(PROTOCOL_KEY, protocol);
    }
    
    private String getProtocol() {
        return (String)body.get(PROTOCOL_KEY);
    }
    */
    
    public void setPackageName(String packageName) {
        body.addContent(NAME_KEY, packageName);
    }
    
    public String getPackageName() {
        return (String)body.get(NAME_KEY);
    }
    
    public void setDepositor(String depositor) {
        body.addContent(DEPOSITOR_KEY, depositor);
    }
    
    public String getDepositor() {
        return (String)body.get(DEPOSITOR_KEY);
    }
    
    public void setLocation(String location) {
        body.addContent(LOCATION_KEY, location);
    }
    
    public String getLocation() {
        return (String)body.get(LOCATION_KEY);
    }
    
    public void setSize(long size) {
        body.addContent(SIZE_KEY, size);
    }
    
    public long getSize() {
        return (long)body.get(SIZE_KEY);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("package-name : ");
        sb.append(getPackageName());
        sb.append(", depositor : ");
        sb.append(getDepositor());
        sb.append(", protocol : ");
        //sb.append(getProtocol());
        sb.append(", location : ");
        sb.append(getLocation());
        sb.append(", size : ");
        sb.append(getSize());
        return sb.toString();
    }
    
}
