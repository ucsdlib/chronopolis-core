/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging;

/**
 *
 * @author toaster
 */
public enum Indicator {
    
    ACK("ack"),
    NAK("nak"),
    QUERY("query");

    private Indicator(String name) {
        this.name = name;
    }
    
    private String name;
    
    public String getName()
    {
        return name;
    }
    
    
}
