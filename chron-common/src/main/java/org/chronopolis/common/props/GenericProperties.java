/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.common.props;

/**
 *  Base layout for a properties file
 *
 * @author shake
 */
public class GenericProperties {
	private String nodeName;
	private String stage;
	
	public GenericProperties(String nodeName, String stage) {
		this.nodeName = nodeName;
		this.stage = stage;
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
} 
