/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.base.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Map;
import org.chronopolis.messaging.MessageType;

/**
 * I got confused with some of the other classes so I made this package to
 * do testing in. This class represents what a message consists of -- the Type
 * for enforcing the correct args, the header, and the body.
 * @author shake
 */
/*
 * TODO: Make MessageType static in each type of message
 * TODO: Create class prototypes here
 * TODO: Header sets in this class too
 */
public abstract class ChronMessage2 {
    public MessageType type;
    public ChronHeader header;
    public ChronBody body;
    
    public void setHeader(Map<String, Object> header) {
        this.header = new ChronHeader(header);
    }

    public void setBody(MessageType type,ChronBody body) {
        this.body = new ChronBody(type, body);
    }

    /*
     * We want this method to return a byte array for use by RabbitMQ. The byte
     * array consists ObjectOutputStream with a ChronBody as the data. In addition,
     * the CorrelationID and Timestamp should be generated at this time for
     * the ChronHeader
     *
     */
    public final byte[] createMessage() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(body);
        return baos.toByteArray();
    }

    // Basic method all subclasses will inherit to process their message
    public abstract void processMessage();

    public final Map<String, Object> getHeader() {
        return header.getHeader();
    }

}
