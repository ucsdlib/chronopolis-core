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
 * 
 * @author toaster
 */
public enum MessageType {

    // DPN Messages
    O_INGEST_INIT_QUERY(MessageState.ORIGIN, ProcessType.INGEST, "init", Indicator.QUERY, "size", "protocol"),
    R_INGEST_AVAIL_ACK(MessageState.RESPONSE, ProcessType.INGEST, "avail", Indicator.ACK),
    R_INGEST_AVAIL_NAK(MessageState.RESPONSE, ProcessType.INGEST, "avail", Indicator.NAK),
    // Ingest <--> Distribute Messages,
	// Do these messages need a query and response? There is only an ack associated
	// with them
    O_DISTRIBUTE_COLL_INIT(MessageState.ORIGIN, ProcessType.DISTRIBUTE, "init", Indicator.QUERY, 
        "depositor", "collection", "tokenStore", "audit.period"),
    R_DISTRIBUTE_INIT_ACK(MessageState.RESPONSE, ProcessType.DISTRIBUTE, "fin", Indicator.ACK),
    O_DISTRIBUTE_TRANSFER_REQUEST(MessageState.ORIGIN, ProcessType.DISTRIBUTE, "transfer", Indicator.QUERY,
        "depositor", "filename", "digest-type", "digest", "location"),
    R_DISTRIBUTE_TRANSFER_ACK(MessageState.ORIGIN, ProcessType.DISTRIBUTE, "complete", Indicator.ACK),
    R_DISTRIBUTE_TRANSFER_NAK(MessageState.ORIGIN, ProcessType.DISTRIBUTE, "complete", Indicator.NAK);
	// Distribute <--> Distribute,
	// Will query other nodes to ask for files and get a response
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
            case "o-ingest-init-query":
                return O_INGEST_INIT_QUERY;
            case "r-ingest-avail-ack":
                return R_INGEST_AVAIL_ACK;
            case "r-ingest-avail-nak":
                return R_INGEST_AVAIL_NAK;
            case "o-distribute-coll-init":
                return O_DISTRIBUTE_COLL_INIT;
            case "r-distribute-init-ack":
                return R_DISTRIBUTE_INIT_ACK;
            case "o-distribute-transfer-request":
                return O_DISTRIBUTE_TRANSFER_REQUEST;
            case "r-distribute-transfer-ack":
                return R_DISTRIBUTE_TRANSFER_ACK;
            case "r-distribute-transfer-nak":
                return R_DISTRIBUTE_TRANSFER_NAK;
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
