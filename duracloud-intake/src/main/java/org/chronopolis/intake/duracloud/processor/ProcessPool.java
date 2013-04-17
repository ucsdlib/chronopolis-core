/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.intake.duracloud.processor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import org.apache.commons.lang3.StringUtils;

/**
 * Web application lifecycle listener.
 * @author toaster
 */
@WebListener()
public class ProcessPool implements ServletContextListener {

    private static final String THREADPOOLSIZE = "procdessorPool";
    private static int DEFAULTSIZE = 10;
    private static ExecutorService service;
    private static boolean shutdown = true;

    public static boolean isShutdown() {
        return shutdown;
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String poolSize = sce.getServletContext().getInitParameter(THREADPOOLSIZE);
        if (StringUtils.isNotEmpty(poolSize)) {
            service = Executors.newFixedThreadPool(Integer.parseInt(poolSize));
        } else {
            service = Executors.newFixedThreadPool(DEFAULTSIZE);
        }
        shutdown = false;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        service.shutdownNow();
        
        shutdown = true;
    }
}
