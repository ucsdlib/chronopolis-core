/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging;

/**
 *
 * @author shake
 */
public enum MessageConstant {
    // Headers
    ORIGIN("origin"),
    RETURN_KEY("returnKey"),
    CORRELATION_ID("correlationId"),
    DATE("date");
    
    private final String text;

    MessageConstant(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
