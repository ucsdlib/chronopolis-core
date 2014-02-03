package org.chronopolis.messaging.pkg;

import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronMessage;

import static org.chronopolis.messaging.MessageConstant.PACKAGE_NAME;
import static org.chronopolis.messaging.MessageConstant.MESSAGE_ATT;

/**
 * Created by shake on 2/3/14.
 */
public class PackageReadyReplyMessage extends ChronMessage {

    public PackageReadyReplyMessage() {
        super(MessageType.PACKAGE_INGEST_READY_REPLY);
    }

    public void setPackageName(String packageName) {
        body.addContent(PACKAGE_NAME.toString(), packageName);
    }

    public void setMessageAtt(Indicator att) {
        body.addContent(MESSAGE_ATT.toString(), att.getName());
    }

    public String getPackageName() {
        return (String) body.get(PACKAGE_NAME.toString());
    }

    public String getMessageAtt() {
        return (String) body.get(MESSAGE_ATT.toString());
    }




}
