package org.chronopolis.amqp;
import org.chronopolis.messaging.ChronMessage;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * Listener which receives notifications from AMQP when chronopolis messages are
 * received by various services
 * 
 * @author toaster
 */
public interface ChronMessageListener {
    
    public void messageReceived(ChronMessage message);
}
