/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.log4j.Logger;

/**
 *
 * @author shake
 */
public class ReplicationContextListener implements ServletContextListener {
    private static final Logger log = Logger.getLogger(ReplicationContextListener.class);
    private ExecutorService serv = Executors.newCachedThreadPool();
    // Check out this awesome variable name
    private Future beep;

    public void contextInitialized(ServletContextEvent sce) {
        log.info("Starting up Replication context");
        ReplicationQueue queue = new ReplicationQueue();
        beep = serv.submit(queue);
        //service.execute(queue);
    }

    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Shutting down Replication context");
        if ( !beep.isCancelled()) {
            beep.cancel(true);
        }
        serv.shutdownNow();
    }
    
}
