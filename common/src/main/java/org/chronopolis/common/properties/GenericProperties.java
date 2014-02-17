/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.common.properties;

/**
 *  Base layout for a properties file
 *
 * @author shake
 */
public class GenericProperties {
	private String nodeName;
	private String stage;
    private String exchange;
    private String inboundKey;
    private String broadcastKey;

	public GenericProperties(String nodeName, String stage, String exchange, String inboundKey, String broadcastKey) {
		this.nodeName = nodeName;
		this.stage = stage;
        this.setExchange(exchange);
        this.setInboundKey(inboundKey);
        this.setBroadcastKey(broadcastKey);
	}

	/**
	 * @return the nodeName
	 */
	public String getNodeName() {
		return nodeName;
	}

	/**
	 * @param nodeName the nodeName to set
	 */
	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	/**
     * TODO: Return a Path instead of a string?
     *
	 * @return the stage
	 */
	public String getStage() {
		return stage;
	}

	/**
	 * @param stage the stage to set
	 */
	public void setStage(String stage) {
		this.stage = stage;
	}

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getInboundKey() {
        return inboundKey;
    }

    public void setInboundKey(String inboundKey) {
        this.inboundKey = inboundKey;
    }

    public String getBroadcastKey() {
        return broadcastKey;
    }

    public void setBroadcastKey(String broadcastKey) {
        this.broadcastKey = broadcastKey;
    }
}
