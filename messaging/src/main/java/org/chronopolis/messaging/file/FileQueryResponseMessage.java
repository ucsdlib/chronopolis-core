/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.file;

import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage;

import static org.chronopolis.messaging.MessageConstant.DEPOSITOR;
import static org.chronopolis.messaging.MessageConstant.PROTOCOL;
import static org.chronopolis.messaging.MessageConstant.FILENAME;
import static org.chronopolis.messaging.MessageConstant.LOCATION;

/**
 *
 * @author shake
 */
public class FileQueryResponseMessage extends ChronMessage {
    private final String DEPOSITOR_KEY = "depositor";
    private final String PROTOCOL_KEY = "protocol";
    private final String FILENAME_KEY = "filename";
    private final String LOCATION_KEY = "location";

    public FileQueryResponseMessage() {
        super(MessageType.FILE_QUERY_RESPONSE);
        this.body = new ChronBody(type);
    }


}
