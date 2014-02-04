package org.chronopolis.messaging.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.rabbitmq.tools.json.JSONSerializable;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.collection.CollectionInitCompleteMessage;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.collection.CollectionInitReplyMessage;
import org.chronopolis.messaging.exception.InvalidMessageException;
import org.chronopolis.messaging.file.FileQueryMessage;
import org.chronopolis.messaging.file.FileQueryResponseMessage;
import org.chronopolis.messaging.pkg.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import static org.chronopolis.messaging.MessageConstant.CORRELATION_ID;
import static org.chronopolis.messaging.MessageConstant.ORIGIN;
import static org.chronopolis.messaging.MessageConstant.DATE;
import static org.chronopolis.messaging.MessageConstant.RETURN_KEY;

/**
 * This class represents what a message consists of -- the Type
 * for enforcing the correct args, the header, and the body.
 *
 *
 * @author shake
 */
public class ChronMessage {
    protected ChronBody body;
    protected final MessageType type;

    // Headers
    private String origin;
    private String returnKey;
    private String correlationId;
    private String date;

    public ChronMessage(MessageType type) {
        this.type = type;
        this.body = new ChronBody(type);
    }
    
    public void setHeader(Map<String, Object> header) {
        this.origin = (String) header.get(ORIGIN.toString());
        this.returnKey= (String) header.get(RETURN_KEY.toString());
        this.correlationId = (String) header.get(CORRELATION_ID.toString());
        this.date = (String) header.get(DATE.toString());
    }

    public void setBody(ChronBody body) {
        if ( type != body.getType() ) {
            throw new RuntimeException("Cannot set body of differing message type ("
                    + body.getType() + ")");
        }

        this.body = new ChronBody(type, body);
    }

    public final Map<String, Object> getHeader() {
        HashMap<String, Object> header = new HashMap<>();
        header.put(ORIGIN.toString(), origin);
        header.put(RETURN_KEY.toString(), returnKey);
        header.put(CORRELATION_ID.toString(), correlationId);
        header.put(DATE.toString(), date);
        return header;
    }

    public final ChronBody getChronBody() {
        return body;
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
        // header.setDate(new Date().toString());
        if ( correlationId == null ) {
            setCorrelationId(UUID.randomUUID().toString());
        }
        return baos.toByteArray();
    }

    public final String toJson() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationConfig(objectMapper.getSerializationConfig()
                .withSerializationInclusion(JsonSerialize.Inclusion.NON_NULL));
        try {
            return objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new InvalidMessageException(body.toString(), e);
        }
    }

    // Helper for returning the type of message we want
    public static ChronMessage getMessage(MessageType type) {
        switch (type) {
            case FILE_QUERY:
                return new FileQueryMessage(); 
            case FILE_QUERY_RESPONSE:
                return new FileQueryResponseMessage();
            case COLLECTION_INIT:
                return new CollectionInitMessage();
            case COLLECTION_INIT_REPLY:
                return new CollectionInitReplyMessage();
            case COLLECTION_INIT_COMPLETE:
                return new CollectionInitCompleteMessage();
            case PACKAGE_INGEST_READY:
                return new PackageReadyMessage();
            case PACKAGE_INGEST_READY_REPLY:
                return new PackageReadyReplyMessage();
            case PACKAGE_INGEST_COMPLETE:
                return new PackageIngestCompleteMessage();
            case PACKAGE_INGEST_STATUS_QUERY:
                return new PackageIngestStatusQueryMessage();
            case PACKAGE_INGEST_STATUS_RESPONSE:
                return new PackageIngestStatusResponseMessage();

            default:
                throw new UnexpectedMessageTypeException(type.toString());

        }
    }

    private static class UnexpectedMessageTypeException extends RuntimeException {
        public UnexpectedMessageTypeException(String toString) {
            super(toString);
        }
    }

    // Header methods 
    public final void setOrigin(String origin) {
        this.origin = origin;
    }

    public final void setReturnKey(String returnKey) {
        this.returnKey = returnKey;
    }

    public final void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public final void setDate(String date) {
        this.date = date;
    }

    public final String getOrigin() {
        return origin;
    }

    public final String getReturnKey() {
        return returnKey;
    }

    public final String getCorrelationId() {
        return correlationId;
    }

    public final String getDate() {
        return date;
    }
}
