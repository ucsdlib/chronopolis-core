/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.base;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.chronopolis.messaging.MessageType;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

/**
 * The body of the chron message. It needs to be serializable so that we can
 * send the contents in the AMQP message.
 * @author shake
 */

@JsonDeserialize(using = ChronBodyDeserializer.class)
public class ChronBody implements Serializable {
    // The body is just a map of keys to values
    // We may want to change it to <String, Object> because we will send back a 
    // list of failed objects
    private Map<String, Object> body = new ConcurrentHashMap<>();
    private final MessageType type;
    
    public ChronBody(MessageType type) {
        this.type = type;
    }

    /*
    public ChronBody(Map<String, Object> body, MessageType type) {
        if (!type.getArgs().containsAll(body.keySet())) {
            throw new IllegalArgumentException("Invalid Key");
        }
        this.body.putAll(body);
        this.type = type;
    }
    */

    public void setBody(Map<String, Object> body) {
        if ( !type.getArgs().containsAll(body.keySet())) {
            System.out.println("oops");
            throw new IllegalArgumentException("Body contains invalid keys");
        }

        this.body = body;
    }

    public ChronBody(MessageType type, ChronBody body) {
        if(!type.getArgs().containsAll(body.getBody().keySet())) {
            throw new IllegalArgumentException("Body contains invalid key(s)");
        }
        this.type = type;
        this.body.putAll(body.getBody());
    }

    public void addContent(String key, Object value) {
        if ( !type.getArgs().contains(key)) {
            throw new IllegalArgumentException("Type of value " + key + " not allowed");
        }

        body.put(key, value);
    }

    public Object get(String key) {
        return body.get(key);
    }

    public MessageType getType() {
        return type;
    }

    public Map<String, Object> getBody() {
       return body; 
    }

    public boolean equals(ChronBody other) {
        if (!body.equals(other.body)) {
            for (Map.Entry<String, Object> e : body.entrySet()) {
                String k = e.getKey();
                Object v = e.getValue();
                System.out.println(k + " : " + v + " == " + other.get(k) + " ? " + v.equals(other.get(k)));
            }
            return false;
        }

        return true;
    }
}
