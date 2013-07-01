/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.logger.processor;

import java.util.Map;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.base.ChronProcessor;

/**
 *
 * @author shake
 */
public class GenericMessageProcessor implements ChronProcessor {

    public void process(ChronMessage2 chronMessage) {
        // All we need to do is create an entity and push these to the DB 
        String messageType = chronMessage.getType().toString();
        Map<String, Object> header = chronMessage.getHeader();
        Map<String, Object> body = chronMessage.getChronBody().getBody();


    }

}
