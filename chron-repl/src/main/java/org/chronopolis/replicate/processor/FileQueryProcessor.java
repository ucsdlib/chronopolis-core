/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.processor;

import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.file.FileQueryMessage;

/**
 *
 * @author shake
 */
public class FileQueryProcessor implements ChronProcessor {

    public void process(ChronMessage2 chronMessage) {
        if (!(chronMessage instanceof FileQueryMessage)) {
            // error out
        }
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
