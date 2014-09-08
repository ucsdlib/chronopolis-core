package org.chronopolis.replicate.util;

import org.chronopolis.messaging.collection.CollectionInitMessage;

import javax.annotation.Nonnull;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

/**
 * Created by shake on 6/20/14.
 */
public class MailFunctions {

    public static String createText(@Nonnull CollectionInitMessage message,
                                    @Nonnull Map<String, String> completionMap,
                                    Exception exception) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter textBody = new PrintWriter(stringWriter, true);
        textBody.println("Message received from: " + message.getOrigin());
        textBody.println(message.toString());

        /*
        textBody.println("\n\nSteps completed:");
        for (Map.Entry entry : completionMap.entrySet()) {
            textBody.println(entry.getKey() + ": " + entry.getValue());
        }
        */

        if (exception != null) {
            textBody.println("\n\nError: \n" + exception.toString());
        }

        return stringWriter.toString();
    }
}
