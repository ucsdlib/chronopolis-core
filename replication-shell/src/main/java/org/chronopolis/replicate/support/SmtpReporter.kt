package org.chronopolis.replicate.support

import com.google.common.hash.Hashing
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentSkipListSet

/**
 * Replacement for the old MailUtil. Currently under the replication-shell module as no other
 * modules use this.
 *
 * @since 3.1.0
 * @author shake
 */
class SmtpReporter(val properties: SmtpProperties) : Reporter<SimpleMailMessage> {
    val log: Logger = LoggerFactory.getLogger(SmtpReporter::class.java)
    private val sent = ConcurrentSkipListSet<String>()

    override fun createMessage(node: String, subject: String, message: String): SimpleMailMessage {
        val smm = SimpleMailMessage()
        smm.setTo(properties.to)
        smm.from = "chron-replication"
        smm.subject = "[$node] $subject"
        smm.text = message
        return smm
    }

    override fun send(message: SimpleMailMessage) {
        val hash = Hashing.murmur3_128().hashString(message.subject?: "", Charset.defaultCharset())

        if (properties.send && sent.add(hash.toString())) {
            log.debug("Sending mail with subject {}", message.subject)
            val sender = JavaMailSenderImpl()
            sender.host = properties.host
            sender.send(message)
        }
    }

}

