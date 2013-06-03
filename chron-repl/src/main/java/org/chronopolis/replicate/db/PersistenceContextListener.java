/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate.db;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

/**
 * Web application lifecycle listener.
 *
 * @author toaster
 */
public class PersistenceContextListener implements ServletContextListener {

     @Override
    public void contextInitialized( ServletContextEvent arg0 ) {
    }

    @Override
    public void contextDestroyed( ServletContextEvent arg0 ) {
        Dba.closeFactory();
    }
}
