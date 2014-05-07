/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.amqp;

import org.chronopolis.messaging.base.ChronMessage;

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
    void send(ChronMessage message, String routingKey);

}
