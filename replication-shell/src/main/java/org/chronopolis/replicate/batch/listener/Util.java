package org.chronopolis.replicate.batch.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.settings.ChronopolisSettings;
import org.chronopolis.rest.entities.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Formatter;
import java.util.List;

/**
 * Created by shake on 6/1/15.
 */
public class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);

    public static void sendFailure(MailUtil mail,
                                   ChronopolisSettings settings,
                                   Replication replication,
                                   List<Throwable> throwables) {
        Formatter titleFormat = new Formatter();
        String title = "Replication failed for collection %s";
        titleFormat.format(title, replication.getBag().getName());

        ObjectMapper mapper = new ObjectMapper();
        StringWriter stringWriter = new StringWriter();
        PrintWriter textBody = new PrintWriter(stringWriter, true);
        try {
            textBody.println(mapper.writeValueAsString(replication));
        } catch (JsonProcessingException e) {
            log.info("Error writing replication as json", e);
            // ignore
        }
        textBody.println();
        textBody.println();
        textBody.println("Exceptions: \n");
        for (Throwable t : throwables) {
            textBody.println(t.getMessage());
            for (StackTraceElement element : t.getStackTrace()) {
                textBody.println(element);
            }
            textBody.println();
        }

        mail.send(mail.createMessage(
                settings.getNode(),
                titleFormat.toString(),
                stringWriter.toString()
        ));
    }

}
