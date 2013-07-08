/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.pkg;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage2;

import static org.chronopolis.messaging.MessageConstant.PACKAGE_NAME;
import static org.chronopolis.messaging.MessageConstant.FAILED_ITEMS;
import static org.chronopolis.messaging.MessageConstant.STATUS;
/**
 *
 * @author shake
 */
public class PackageIngestCompleteMessage extends ChronMessage2 {

    public PackageIngestCompleteMessage() {
        super(MessageType.PACKAGE_INGEST_COMPLETE);
        this.body = new ChronBody(type);
    }

    public void setPackageName(String packageName) {
        body.addContent(PACKAGE_NAME.toString(), packageName);
    }
    
    public String getPackageName() {
        return (String) body.get(PACKAGE_NAME.toString());
    }

    public void setStatus(String status) {
        body.addContent(PACKAGE_NAME.toString(), status);
    }

    public String getStatus() {
        return (String) body.get(STATUS.toString());
    }

    /* While I don't expect us to access this from multiple threads, I think it's
     * important to note that this is in no way thread safe despite using a nice
     * ArrayList. Not only could we have multiple instantiations (both threads having
     * a null item at the branch), but I do not know what the behavior would look like
     * for multiple accesses. I'll test it out soon enough and update, should be interesting.
     */
    public void setFailedItem(String item) {
        List<String> items = (List<String>) body.get(FAILED_ITEMS.toString());
        if ( items == null ) {
            items = new CopyOnWriteArrayList<>();
        }
        items.add(item);
        body.addContent(FAILED_ITEMS.toString(), items);
    }

    public List<String> getFailedItems() {
        return (List<String>) body.get(FAILED_ITEMS.toString());
    }

}
