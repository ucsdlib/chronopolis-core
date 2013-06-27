/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.amqp;

import org.apache.log4j.Logger;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionListener;

/**
 *
 * @author shake
 */
public class ConnectionListenerImpl implements ConnectionListener {
    private final Logger log = Logger.getLogger(ConnectionListenerImpl.class);

    public void onCreate(Connection cnctn) {
        log.info("Connection created " + cnctn.toString());
    }

    public void onClose(Connection cnctn) {
        log.info("Connection closed" + cnctn.toString());
    }
    
}
