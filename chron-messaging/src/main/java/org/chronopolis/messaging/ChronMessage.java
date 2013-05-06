/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging;

import java.util.Date;
import java.util.Map;

/**
 * Messages are immutable once created
 * 
 * @author toaster
 */
public abstract class ChronMessage {
    // Header Types
    private String src;
    private String returnKey;
    private String correlationId;
    private Date date;

    // Body Types
    private MessageType messageName;
    private Map<String, String> messageArgs;

    public MessageType getMessageName() {
        return messageName;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Date getDate() {
        return date;
    }

    public String getReturnKey() {
        return returnKey;
    }

    public String getSrc() {
        return src;
    }

    protected ChronMessage(MessageType type) {
    }

    public final String serializeToJSON() {
        return null;
    }

    public String get(String key) {
        if (!messageName.getArgs().contains(key)) {
            throw new IllegalArgumentException("Invalid key for message type " + messageName);
        }
        return messageArgs.get(key);
    }

    public Long getLong(String key) {
        String value = get(key);

        if (null == value || value.isEmpty()) {
            return null;
        } else {
            return Long.valueOf(value);
        }
    }

    public Integer getInt(String key) {
        String value = get(key);

        if (null == value || value.isEmpty()) {
            return null;
        } else {
            return Integer.valueOf(value);
        }
    }
}
