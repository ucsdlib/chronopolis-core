/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * @author toaster
 */
public class MessageBuilder {

    //header
    private String src;
    private String returnKey;
    //body
    private MessageType messageName;
    private Map<String, String> body = new ConcurrentHashMap<>();

    public MessageType getMessageName() {
        return messageName;
    }

    public MessageBuilder setMessageName(MessageType messageName) {
        this.messageName = messageName;
        return this;
    }

    public MessageBuilder setReturnKey(String returnKey) {
        this.returnKey = returnKey;
        return this;
    }

    public MessageBuilder setSrc(String src) {
        this.src = src;
        return this;
    }

    /**
     * sets a body's property property
     * @param key
     * @param value
     * @throws IllegalStateException when message name has not been set
     * @throws IllegalArgumentException when key is not allowed for message name
     */
    public MessageBuilder set(String key, Object value) {
        if (messageName == null) {
            throw new IllegalStateException("Message Name not set");
        }
        if (!messageName.getArgs().contains(key)) {
            throw new IllegalArgumentException("Key " + key + " not allowed for " + messageName);
        }
        body.put(key, value.toString());
        return this;
    }

    public String getReturnKey() {
        return returnKey;
    }

    public String getSrc() {
        return src;
    }

    public void reset() {
        src = null;
        returnKey = null;
        messageName = null;
        body.clear();
    }

    public String createMessage() {
        //TODO
        // autogenerate coor ID
        // set date to current date
        return null;
    }
}
