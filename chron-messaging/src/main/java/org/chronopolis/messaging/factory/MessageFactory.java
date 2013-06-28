/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.factory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import org.chronopolis.messaging.base.ChronHeader;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.file.FileQueryMessage;
import org.chronopolis.messaging.file.FileQueryResponseMessage;
import org.chronopolis.messaging.pkg.PackageIngestCompleteMessage;
import org.chronopolis.messaging.pkg.PackageIngestStatusQueryMessage;
import org.chronopolis.messaging.pkg.PackageIngestStatusResponseMessage;
import org.chronopolis.messaging.pkg.PackageReadyMessage;

/**
 * TODO: Order based on length of method names heh
 *
 * @author shake
 */
public class MessageFactory {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssz");
    
    private static void setHeaders(ChronHeader headers) {
        headers.setDate(dateFormat.format(new Date()));
        headers.setReturnKey("key.umiacs");
        headers.setOrigin("umiacs");
        headers.setCorrelationId(UUID.randomUUID().toString());
    }

    public static CollectionInitMessage DefaultCollectionInitMessage() {
        CollectionInitMessage msg = new CollectionInitMessage();
        msg.setAuditPeriod("90");
        msg.setCollection("default-collection");
        msg.setDepositor("default-depositor");
        msg.setTokenStore("https://default/tokenstore");
        setHeaders(msg.getChronHeader());
        return msg;
    }

    public static FileQueryMessage DefaultFileQueryMessage() { 
        FileQueryMessage msg = new FileQueryMessage();
        return msg;
    } 

    public static FileQueryResponseMessage DefaultFileQueryResponseMessage() {
        FileQueryResponseMessage msg = new FileQueryResponseMessage();  
        return msg;
    }

    public static PackageReadyMessage DefaultPackageReadyMessage() {
        PackageReadyMessage msg = new PackageReadyMessage();
        msg.setDepositor("default-depositor");
        msg.setLocation("default-location");
        msg.setPackageName("default-package-name");
        msg.setSize(1024);
        setHeaders(msg.getChronHeader());
        return msg;
    } 

    public static PackageIngestCompleteMessage DefaultPackageIngestCompleteMessage() {
        PackageIngestCompleteMessage msg = new PackageIngestCompleteMessage();
        return msg;
    }

    public static PackageIngestStatusQueryMessage DefaultPackageIngestStatusQueryMessage() {
        PackageIngestStatusQueryMessage msg = new PackageIngestStatusQueryMessage();
        return msg;
    }

    public static PackageIngestStatusResponseMessage DefaultPackageIngestStatusResponseMessage() {
        PackageIngestStatusResponseMessage msg = new PackageIngestStatusResponseMessage();  
        return msg;
    }
    
}
