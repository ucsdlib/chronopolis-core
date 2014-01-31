/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest;

import org.chronopolis.common.properties.GenericProperties;

/**
 *
 * @author shake
 */
public class IngestProperties extends GenericProperties{
	private String tokenStage;
	private String imsHostName;

	/**
	 *
	 * @param nodeName
	 * @param bagStage
	 * @param tokenStage
	 * @param imsHostName
	 */
	public IngestProperties(String nodeName,
                            String bagStage,
                            String exchange,
                            String inboundKey,
                            String broadcastKey,
                            String tokenStage,
                            String imsHostName) {
		super(nodeName, bagStage, exchange, inboundKey, broadcastKey);
		this.tokenStage = tokenStage;
		this.imsHostName = imsHostName;
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

	private String getErrorMessage() {
		return "Error in ingest.properties";
	}
	 
}
