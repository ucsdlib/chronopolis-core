package org.chronopolis.replicate.support

/**
 * So we can mock the [SmtpReporter]
 *
 * @since 3.1.0
 * @author shake
 */
interface Reporter<T> {
    fun send(message: T)
    fun createMessage(node: String, subject: String, message: String): T
}