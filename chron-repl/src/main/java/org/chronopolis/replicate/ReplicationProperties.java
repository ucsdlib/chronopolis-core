/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.replicate;

import org.chronopolis.common.properties.GenericProperties;

/**
 *
 * @author shake
 */
public class ReplicationProperties extends GenericProperties{
    private String aceFqdn;
    private String acePath;
    private int acePort;

    public ReplicationProperties(String nodeName, String stage, String aceFqdn, 
                                 String acePath, int acePort) {
        super(nodeName, stage);
        this.aceFqdn = aceFqdn;
        this.acePath = acePath;
        this.acePort = acePort;
    }

    /**
     * @return the aceFqdn
     */
    public String getAceFqdn() {
        return aceFqdn;
    }

    /**
     * @param aceFqdn the aceFqdn to set
     */
    public void setAceFqdn(String aceFqdn) {
        this.aceFqdn = aceFqdn;
    }

    /**
     * @return the acePath
     */
    public String getAcePath() {
        return acePath;
    }

    /**
     * @param acePath the acePath to set
     */
    public void setAcePath(String acePath) {
        this.acePath = acePath;
    }

    /**
     * @return the acePort
     */
    public int getAcePort() {
        return acePort;
    }

    /**
     * @param acePort the acePort to set
     */
    public void setAcePort(int acePort) {
        this.acePort = acePort;
    }
    
}
