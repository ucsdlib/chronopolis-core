/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.factory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.collection.CollectionInitCompleteMessage;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.file.FileQueryMessage;
import org.chronopolis.messaging.file.FileQueryResponseMessage;
import org.chronopolis.messaging.pkg.PackageIngestCompleteMessage;
import org.chronopolis.messaging.pkg.PackageIngestStatusQueryMessage;
import org.chronopolis.messaging.pkg.PackageIngestStatusResponseMessage;
import org.chronopolis.messaging.pkg.PackageReadyMessage;

/**
 * TODO: Order based on length of method names heh
 * TODO: Move to JodaTime
 *
 * @author shake
 */
public class MessageFactory {

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ssz");
    
    private static void setHeaders(ChronMessage msg) {
        msg.setDate(dateFormat.format(new Date()));
        msg.setReturnKey("key.umiacs");
        msg.setOrigin("umiacs");
        msg.setCorrelationId(UUID.randomUUID().toString());
    }

    public static CollectionInitMessage DefaultCollectionInitMessage() {
        CollectionInitMessage msg = new CollectionInitMessage();
        msg.setAuditPeriod(90);
        msg.setCollection("default-collection");
        msg.setDepositor("default-depositor");
        msg.setTokenStore("https://localhost/tokenstore");
        setHeaders(msg);
        return msg;
    }

    public CollectionInitMessage collectionInitMessage(long auditPeriod, String collection, String depositor, String tokenStore) {
        CollectionInitMessage msg = new CollectionInitMessage();
        msg.setAuditPeriod(auditPeriod);
        msg.setCollection(collection);
        msg.setDepositor(depositor);
        msg.setTokenStore(tokenStore);
        setHeaders(msg);
        return msg;
    }

    public static CollectionInitCompleteMessage DefaultCollectionInitCompleteMessage() {
        CollectionInitCompleteMessage msg = new CollectionInitCompleteMessage();
        setHeaders(msg);
        return msg;
    }


    public static FileQueryMessage DefaultFileQueryMessage() { 
        FileQueryMessage msg = new FileQueryMessage();
        setHeaders(msg);
        return msg;
    } 

    public static FileQueryResponseMessage DefaultFileQueryResponseMessage() {
        FileQueryResponseMessage msg = new FileQueryResponseMessage();  
        setHeaders(msg);
        return msg;
    }

    public static PackageReadyMessage DefaultPackageReadyMessage() {
        PackageReadyMessage msg = new PackageReadyMessage();
        msg.setDepositor("default-depositor");
        msg.setLocation("default-location");
        msg.setPackageName("default-package-name");
        msg.setFixityAlgorithm("sha-256");
        msg.setSize(1024);
        setHeaders(msg);
        return msg;
    }

    public PackageReadyMessage packageReadyMessage(String depositor,
                                                   String fixityAlg,
                                                   String location,
                                                   String packageName,
                                                   long size) {
        PackageReadyMessage msg = new PackageReadyMessage();
        msg.setDepositor(depositor);
        msg.setFixityAlgorithm(fixityAlg);
        msg.setLocation(location);
        msg.setPackageName(packageName);
        msg.setSize(size);
        setHeaders(msg);
        return msg;
    }


    public static PackageIngestCompleteMessage DefaultPackageIngestCompleteMessage() {
        PackageIngestCompleteMessage msg = new PackageIngestCompleteMessage();
        setHeaders(msg);
        return msg;
    }

    public static PackageIngestStatusQueryMessage DefaultPackageIngestStatusQueryMessage() {
        PackageIngestStatusQueryMessage msg = new PackageIngestStatusQueryMessage();
        setHeaders(msg);
        return msg;
    }

    public static PackageIngestStatusResponseMessage DefaultPackageIngestStatusResponseMessage() {
        PackageIngestStatusResponseMessage msg = new PackageIngestStatusResponseMessage();  
        setHeaders(msg);
        return msg;
    }
    
}
