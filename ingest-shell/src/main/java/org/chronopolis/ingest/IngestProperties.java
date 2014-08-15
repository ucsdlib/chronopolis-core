/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest;

import org.chronopolis.amqp.RoutingKey;
import org.chronopolis.common.properties.GenericProperties;

import java.util.List;

/**
 *
 * @author shake
 */
public class IngestProperties extends GenericProperties {

    private final String directQueueName;
    private final String directQueueBinding;
    private final String broadcastQueueName;
    private final String broadcastQueueBinding;

    private String tokenStage;
    private String imsHostName;
    private String storageServer;
    private String externalUser;
    private String preservation;
    private Boolean dpnPush;
    private List<String> chronNodes;

    /**
     *  @param nodeName
     * @param bagStage
     * @param tokenStage
     * @param imsHostName
     * @param preservation
     */
    public IngestProperties(String nodeName,
                            String bagStage,
                            String exchange,
                            String inboundKey,
                            String broadcastKey,
                            String tokenStage,
                            String imsHostName,
                            String storageServer,
                            String externalUser,
                            String preservation,
                            Boolean dpnPush,
                            List<String> chronNodes) {
        super(nodeName, bagStage, exchange, inboundKey, broadcastKey);
        this.tokenStage = tokenStage;
        this.imsHostName = imsHostName;
        this.storageServer = storageServer;
        this.externalUser = externalUser;
        this.preservation = preservation;
        this.dpnPush = dpnPush;
        this.chronNodes = chronNodes;

        directQueueName = "ingest.direct." + nodeName;
        broadcastQueueName = "ingest.broadcast." + nodeName;
        directQueueBinding = "ingest-" + nodeName + "-inbound";
        broadcastQueueBinding = RoutingKey.INGEST_BROADCAST.asRoute();

        setInboundKey(directQueueBinding);
    }

    /**
     * @return the tokenStage
     */
    public String getTokenStage() {
        return tokenStage;
    }

    /**
     * @param tokenStage the tokenStage to set
     */
    public void setTokenStage(String tokenStage) {
        this.tokenStage = tokenStage;
    }

    /**
     * @return the imsHostName
     */
    public String getImsHostName() {
        return imsHostName;
    }

    /**
     * @param imsHostName the imsHostName to set
     */
    public void setImsHostName(String imsHostName) {
        this.imsHostName = imsHostName;
    }

    public String getStorageServer() {
        return storageServer;
    }

    public void setStorageServer(String storageServer) {
        this.storageServer = storageServer;
    }

    private String getErrorMessage() {
        return "Error in ingest.properties";
    }

    public String getExternalUser() {
        return externalUser;
    }

    public void setExternalUser(String externalUser) {
        this.externalUser = externalUser;
    }

    public Boolean pushToDpn() {
        return dpnPush;
    }

    public void setDpnPush(Boolean dpnPush) {
        this.dpnPush = dpnPush;
    }

    public String getDirectQueueName() {
        return directQueueName;
    }

    public String getDirectQueueBinding() {
        return directQueueBinding;
    }

    public String getBroadcastQueueName() {
        return broadcastQueueName;
    }

    public String getBroadcastQueueBinding() {
        return broadcastQueueBinding;
    }

    public List<String> getChronNodes() {
        return chronNodes;
    }

    public void setChronNodes(final List<String> chronNodes) {
        this.chronNodes = chronNodes;
    }

    public String getPreservation() {
        return preservation;
    }

    public void setPreservation(final String preservation) {
        this.preservation = preservation;
    }

}
