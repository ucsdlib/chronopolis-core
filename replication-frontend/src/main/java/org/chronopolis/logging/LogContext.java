/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.logging;

import java.util.Enumeration;
import java.util.Properties;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author shake
 */
public class LogContext implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Properties log4jProp = new Properties();
        Enumeration<String> e = sce.getServletContext().getInitParameterNames();
        while (e.hasMoreElements()) {
        String key = e.nextElement();
            if (key.startsWith("log4j")) {
                log4jProp.setProperty(key, sce.getServletContext().getInitParameter(key));
            }
            
        }

        PropertyConfigurator.configure(log4jProp);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }
}
