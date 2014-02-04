/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Container for chronopolis/DPN style messages
 * MessageState - The state of the node who produces the message
 * ProcessType - The process type of the message flow
 * Stage -
 * Indicator - The type of message
 * Args - The parameters of the message
 * 
 * TODO: Replace Args w/ MessageConstant... constants...
 *
 * @author toaster
 */
public enum MessageType {
    
    // DPN Messages
    INGEST_INIT_QUERY(MessageState.ORIGIN, ProcessType.INGEST, "init", Indicator.QUERY, "size", "protocol"),
    INGEST_AVAIL_ACK(MessageState.RESPONSE, ProcessType.INGEST, "avail", Indicator.ACK),
    INGEST_AVAIL_NAK(MessageState.RESPONSE, ProcessType.INGEST, "avail", Indicator.NAK),

    // Ingest <--> Distribute Messages,
    // Do these messages need a query and response? There is only an ack associated
    // with them
    COLLECTION_INIT(MessageState.ORIGIN, ProcessType.DISTRIBUTE, "init", Indicator.QUERY,
            "depositor", "collection", "token-store", "audit-period"),
    COLLECTION_INIT_REPLY(MessageState.ORIGIN, ProcessType.DISTRIBUTE, "avail", Indicator.QUERY,
            "message-att"),
    COLLECTION_INIT_COMPLETE(MessageState.RESPONSE, ProcessType.DISTRIBUTE, "ack", Indicator.ACK,
            "collection", "attribute", "error-list"),

    // Distribute <--> Distribute,
    // Will query other nodes to ask for files and get a response
    FILE_QUERY(MessageState.ORIGIN, ProcessType.QUERY, "avail", Indicator.QUERY,
            "depositor", "protocol", "location", "filename"),
    FILE_QUERY_RESPONSE(MessageState.RESPONSE, ProcessType.QUERY, "transfer", Indicator.ACK,
            "available", "protocol", "location"),

    // Intake <--> Ingest  
    PACKAGE_INGEST_READY(MessageState.ORIGIN, ProcessType.INGEST, "init", Indicator.QUERY, 
            "package-name", "location", "depositor", "size"),
    PACKAGE_INGEST_READY_REPLY(MessageState.ORIGIN, ProcessType.INGEST, "ack", Indicator.QUERY,
            "package-name", "message-att"),
    PACKAGE_INGEST_COMPLETE(MessageState.RESPONSE, ProcessType.INGEST, "ack", Indicator.ACK,
            "package-name", "status", "failed-items"),
    PACKAGE_INGEST_STATUS_QUERY(MessageState.RESPONSE, ProcessType.INGEST, "query", Indicator.ACK,
            "package-name", "depositor"), 
    PACKAGE_INGEST_STATUS_RESPONSE(MessageState.RESPONSE, ProcessType.INGEST, "response", Indicator.ACK,
            "status", "completion-percent"), 
    ;

    private MessageState state;
    private ProcessType process;
    private String stage;
    private Indicator indicator;
    private List<String> args;
    
    private MessageType(MessageState state, ProcessType process, String stage, Indicator indicator, String... args) {
        this.state = state;
        this.process = process;
        this.stage = stage;
        this.indicator = indicator;
        this.args = Collections.unmodifiableList(Arrays.asList(args));
    }
    
    public static MessageType decode(String message) {
        if (null == message) {
            throw new NullPointerException();
        }
        
        switch (message.toLowerCase()) {
            case "o_ingest_init_query":
                return INGEST_INIT_QUERY;
            case "r_ingest_avail_ack":
                return INGEST_AVAIL_ACK;
            case "r_ingest_avail_nak":
                return INGEST_AVAIL_NAK;
            case "collection_init":
                return COLLECTION_INIT;
            case "collection_init_complete":
                return COLLECTION_INIT_COMPLETE;
            case "collection_init_reply":
                return COLLECTION_INIT_REPLY;
            case "file_query":
                return FILE_QUERY;
            case "file_query_response":
                return FILE_QUERY_RESPONSE;
            case "package_ingest_complete":
                return PACKAGE_INGEST_COMPLETE;
            case "package_ingest_ready":
                return PACKAGE_INGEST_READY;
            case "package_ingest_ready_reply":
                return PACKAGE_INGEST_READY_REPLY;
            case "package_ingest_status_query":
                return PACKAGE_INGEST_STATUS_QUERY;
            case "package_ingest_status_response":
                return PACKAGE_INGEST_STATUS_RESPONSE;
            default:
                throw new IllegalArgumentException("unknown message name: " + message);
                
        }
    }
    
    public String encode() {
        StringBuilder sb = new StringBuilder();
        sb.append(state.getName()).append('-');
        sb.append(process.getName()).append('-');
        sb.append(stage).append('-');
        sb.append(indicator.getName());
        return sb.toString();
    }
    
    /**
     *
     * @return
     */
    public MessageState getState() {
        return state;
    }
    
    /**
     * List of acceptable argument keys for this message type
     * @return
     */
    public List<String> getArgs() {
        return args;
    }
    
    /**
     * Each of our control flows will have a unique identifier attached to it.  For example, Content Ingest
     * will be "ingest".  Every message exchanged during the control flow / process will be has this tag.
     * ProcessType
     * @return
     */
    public ProcessType getProcess() {
        return process;
    }
    
    /**
     * Refers to the point at which the message is sent in the process.  Unambiguously replaces sequence, and
     * does not lend itself to being implemented with a += 1, a method that provided no benefit.  ProcessType
     * @return
     */
    public String getStage() {
        return stage;
    }
    
    /**
     * ack/nack/query
     * @return
     */
    public Indicator getIndicator() {
        return indicator;
    }

}
