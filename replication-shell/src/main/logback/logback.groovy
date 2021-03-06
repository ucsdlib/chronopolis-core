/**
 * Created by shake on 2/11/14.
 */

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender

import static ch.qos.logback.classic.Level.ERROR
import static ch.qos.logback.classic.Level.TRACE
import static ch.qos.logback.classic.Level.INFO

appender("CONSOLE", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        //pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{5} - %msg%n"
        pattern = "%level %logger{30} - %msg%n"
    }
    filter(ThresholdFilter) {
        level = INFO
    }
}

appender("FILE", FileAppender) {
    file = "replicate.log"
    append = true
    encoder(PatternLayoutEncoder) {
        pattern = "%d{yyyy/MM/dd HH:mm:ss} %level %logger - %msg%n"
    }
}


//logger("org.chronopolis", INFO, ["CONSOLE"])
logger("org.springframework", ERROR)
logger("org.hibernate", INFO)
logger("org.quartz", INFO)
root(TRACE, ["FILE", "CONSOLE"])
