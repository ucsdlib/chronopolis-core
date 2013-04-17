/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.test;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 *
 * @author mike
 */
public class MockContext implements ServletContextListener {
	private MockReceive mr;
	public void contextInitialized(ServletContextEvent sce) {
		mr = new MockReceive();
		mr.start();
	}

	public synchronized void contextDestroyed(ServletContextEvent sce) {
        try {
            System.out.println("Saying goodbye");
            if ( mr.isAlive()) {
                mr.setRun(false);
            }
            mr.wait();
            System.out.println("leaving");
            //mr.interrupt();
        } catch (InterruptedException ex) {
            Logger.getLogger(MockContext.class.getName()).log(Level.SEVERE, null, ex);
        }
	}
	
}
