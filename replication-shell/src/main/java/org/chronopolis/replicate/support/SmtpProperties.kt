package org.chronopolis.replicate.support

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration for smtp reporting
 *
 *
 * @param send configuration for sending mail
 * @param sendOnSuccess configuration for sending mail for successful replications
 * @param to mail address which reports will be sent to
 * @param host host which the mail will originate from
 * @since 3.1.0
 * @author shake
 */
@ConfigurationProperties(prefix = "smtp")
data class SmtpProperties(var send: Boolean = true,
                          var sendOnSuccess: Boolean = true,
                          var to: String = "chronopolis-support-l@ucsd.edu",
                          var host: String = "localhost.localdomain")