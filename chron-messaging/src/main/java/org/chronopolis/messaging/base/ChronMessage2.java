/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
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
public class ChronMessage2 {
    // public MessageType type;
    protected ChronHeader header;
    protected ChronBody body;
    protected final MessageType type;

    public ChronMessage2(MessageType type) {
        this.type = type;
    }
    
    public void setHeader(Map<String, Object> header) {
        this.header = new ChronHeader(header);
    }

    public void setBody(MessageType type,ChronBody body) {
        this.body = new ChronBody(type, body);
    }

    public MessageType getType() { 
        return this.type;
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
        header.setDate(new Date().toString());
        header.setCorrelationId(UUID.randomUUID().toString());
        return baos.toByteArray();
    }

    public final Map<String, Object> getHeader() {
        return header.getHeader();
    }

    public final ChronHeader getChronHeader() {
        return header;
    }

    public final ChronBody getChronBody() {
        return body;
    }

}
