/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.amqp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.Connection;
import org.springframework.amqp.rabbit.connection.ConnectionListener;

/**
 *
 * @author shake
 */
public class ConnectionListenerImpl implements ConnectionListener {
    private final Logger log = LoggerFactory.getLogger(ConnectionListenerImpl.class);

    public void onCreate(Connection cnctn) {
        log.info("Connection created " + cnctn.toString());
    }

    public void onClose(Connection cnctn) {
        log.info("Connection closed" + cnctn.toString());
    }
    
}
