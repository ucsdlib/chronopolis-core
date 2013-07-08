/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.file;

import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronMessage2;

import static org.chronopolis.messaging.MessageConstant.DEPOSITOR;
import static org.chronopolis.messaging.MessageConstant.PROTOCOL;
import static org.chronopolis.messaging.MessageConstant.LOCATION;
import static org.chronopolis.messaging.MessageConstant.FILENAME;

/**
 * Used by the Distribution Service to ask other nodes the status of 
 * a particular file[s]
 *
 * @author shake
 */
public class FileQueryMessage extends ChronMessage2 {

    public FileQueryMessage() {
        super(MessageType.FILE_QUERY);
        this.body = new ChronBody(type);
    }

}
