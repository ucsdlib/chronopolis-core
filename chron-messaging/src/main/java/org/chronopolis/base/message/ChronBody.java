/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.base.message;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.chronopolis.messaging.MessageType;

/**
 * The body of the chron message. It needs to be serializable so that we can
 * send the contents in the AMQP message.
 * @author shake
 */
public class ChronBody implements Serializable {
    // The body is just a map of keys to values
    // We may want to change it to <String, Object> 
    private Map<String, String> body = new ConcurrentHashMap<>();
    private MessageType type;
    
    public ChronBody(MessageType type) {
        this.type = type;
    }

    public ChronBody(MessageType type, Map<String, String> body) {
        if (!type.getArgs().containsAll(body.keySet())) {
            throw new IllegalArgumentException("Invalid Key");
        }
        this.body.putAll(body);
        this.type = type;
    }

    public ChronBody(MessageType type, ChronBody body) {
        if(!type.getArgs().containsAll(body.getBody().keySet())) {
            throw new IllegalArgumentException("Body contains invalid key(s)");
        }
        this.type = type;
        this.body.putAll(body.getBody());
    }

    public void addContent(String key, String value) {
        if ( !type.getArgs().contains(key)) {
            throw new IllegalArgumentException("Type of value " + key + " not allowed");
        }

        body.put(key, value);
    }

    public Map<String, String> getBody() {
       return body; 
    }
}
