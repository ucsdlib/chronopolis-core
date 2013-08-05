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
    // Lots of things related to ACE
    private String aceFqdn;
    private String acePath;
    private String aceUser;
    private String acePass;
    private int acePort = 8080;

    public ReplicationProperties(String nodeName, String stage, String aceFqdn, 
                                 String acePath, String aceUser, String acePass, 
                                 String acePort) {
        super(nodeName, stage);
        this.aceFqdn = aceFqdn;
        this.acePath = acePath;
        this.aceUser = aceUser;
        this.acePass = acePass;
        if ( acePort == null ) {
            System.out.println("WHATTHEFUCKINGSHIT");
        } else {
            System.out.println("WHATTHEFUCKINGSHIT");
        }
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

    /**
     * @return the aceUser
     */
    public String getAceUser() {
        return aceUser;
    }

    /**
     * @param aceUser the aceUser to set
     */
    public void setAceUser(String aceUser) {
        this.aceUser = aceUser;
    }

    /**
     * @return the acePass
     */
    public String getAcePass() {
        return acePass;
    }

    /**
     * @param acePass the acePass to set
     */
    public void setAcePass(String acePass) {
        this.acePass = acePass;
    }
    
}
