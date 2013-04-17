/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.chronopolis.logging;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;

/**
 *
 * @author shake
 */
public class LogContext implements ServletContextListener{
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        Properties log4jProp = new Properties();
        log4jProp.setProperty("log4j.appender.A1.File", "/tmp/chron-repl.log");
        log4jProp.setProperty("log4j.appender.A1", "org.apache.log4j.RollingFileAppender");
        log4jProp.setProperty("log4j.appender.A1.maxFileSize", "100000KB");
        log4jProp.setProperty("log4j.rootLogger", "FATAL, A1");
        log4jProp.setProperty("log4j.appender.A1.layout", "org.apache.log4j.PatternLayout");
        log4jProp.setProperty("log4j.appender.A1.layout.ConversionPattern", "%d{[dd/MMM/yyyy:HH:mm:ss]} %x%m%n");
        PropertyConfigurator.configure(log4jProp);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
    }

}
