/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.chronopolis.replicate;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 * @author shake
 */
public class AMQPContext implements ServletContextListener{
	private AMQPFileConsumer consumer;
    @Override
    public void contextInitialized(ServletContextEvent sce) {
		consumer = new AMQPFileConsumer();
		//consumer.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (consumer.isAlive()) {
            consumer.setRun(false);
        }
		consumer.interrupt();
    }

}
