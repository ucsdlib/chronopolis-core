package org.chronopolis.messaging.base;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.collection.CollectionInitCompleteMessage;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.chronopolis.messaging.collection.CollectionInitReplyMessage;
import org.chronopolis.messaging.collection.CollectionRestoreReplyMessage;
import org.chronopolis.messaging.collection.CollectionRestoreRequestMessage;
import org.chronopolis.messaging.exception.InvalidMessageException;
import org.chronopolis.messaging.file.FileQueryMessage;
import org.chronopolis.messaging.file.FileQueryResponseMessage;
import org.chronopolis.messaging.pkg.PackageIngestCompleteMessage;
import org.chronopolis.messaging.pkg.PackageIngestStatusQueryMessage;
import org.chronopolis.messaging.pkg.PackageIngestStatusResponseMessage;
import org.chronopolis.messaging.pkg.PackageReadyMessage;
import org.chronopolis.messaging.pkg.PackageReadyReplyMessage;
import org.codehaus.jackson.map.ObjectMapper;
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
        this.returnKey = (String) header.get(RETURN_KEY.toString());
        this.correlationId = (String) header.get(CORRELATION_ID.toString());
        this.date = (String) header.get(DATE.toString());
    }

    public void setBody(ChronBody body) {
        if (type != body.getType()) {
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
            case COLLECTION_RESTORE_REQUEST:
                return new CollectionRestoreRequestMessage();
            case COLLECTION_RESTORE_REPLY:
                return new CollectionRestoreReplyMessage();
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final ChronMessage that = (ChronMessage) o;

        if (body != null ? !body.equals(that.body) : that.body != null) return false;
        if (correlationId != null ? !correlationId.equals(that.correlationId) : that.correlationId != null)
            return false;
        if (date != null ? !date.equals(that.date) : that.date != null) return false;
        if (origin != null ? !origin.equals(that.origin) : that.origin != null) return false;
        if (returnKey != null ? !returnKey.equals(that.returnKey) : that.returnKey != null) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = body != null ? body.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (origin != null ? origin.hashCode() : 0);
        result = 31 * result + (returnKey != null ? returnKey.hashCode() : 0);
        result = 31 * result + (correlationId != null ? correlationId.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        return result;
    }

    private static class UnexpectedMessageTypeException extends RuntimeException {
        public UnexpectedMessageTypeException(String toString) {
            super(toString);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{").append("\n\ttype:").append(type.toString()).append("\n");
        for (Map.Entry<String, Object> entry : body.getBody().entrySet()) {
            sb.append("\t").append(entry.getKey()).append(":")
              .append(entry.getValue()).append(",\n");
        }
        sb.append("}");
        return sb.toString();
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
