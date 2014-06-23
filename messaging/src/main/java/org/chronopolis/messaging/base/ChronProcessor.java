/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.base;

/**
 *
 * @author shake
 */
public interface ChronProcessor {

    /**
     * Process {@link org.chronopolis.messaging.base.ChronMessage}'s received
     * through RabbitMQ
     *
     * @param chronMessage the received message
     */
    void process(ChronMessage chronMessage);

}
