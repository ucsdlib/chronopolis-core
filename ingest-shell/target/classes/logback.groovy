/**
 * Created by shake on 2/11/14.
 */

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender

import static ch.qos.logback.classic.Level.DEBUG
import static ch.qos.logback.classic.Level.TRACE
import static ch.qos.logback.classic.Level.INFO

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        //pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{5} - %msg%n"
        pattern = "%level %logger - %msg%n"
    }
}

/*
appender("FILE", FileAppender) {
    println("This is a test")
    file = "ingest.log"
    append = true
    encoder(PatternLayoutEncoder) {
        pattern = "%level %logger - %msg%n"
    }
}
*/


//logger("org.chronopolis", INFO, ["CONSOLE"])
root(DEBUG, ["CONSOLE"])
