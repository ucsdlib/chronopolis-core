/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.amqp;

import org.chronopolis.messaging.base.ChronMessage2;

/**
 *
 * @author shake
 */
public interface ChronProducer {

    /*
     * 
     * @param message       message to be sent
     * @param routingKey    key for the route
     */
    public void send(ChronMessage2 message, String routingKey);
    
}
