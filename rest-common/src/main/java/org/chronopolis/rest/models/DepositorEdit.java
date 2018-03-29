package org.chronopolis.rest.models;

import java.util.List;

/**
 * Beep beep model for editing a Depositor.
 *
 * @author shake
 */
public class DepositorEdit {

    private String sourceOrganization;
    private String organizationAddress;
    private List<String> replicatingNodes;

    public String getSourceOrganization() {
        return sourceOrganization;
    }

    public DepositorEdit setSourceOrganization(String sourceOrganization) {
        this.sourceOrganization = sourceOrganization;
        return this;
    }

    public String getOrganizationAddress() {
        return organizationAddress;
    }

    public DepositorEdit setOrganizationAddress(String organizationAddress) {
        this.organizationAddress = organizationAddress;
        return this;
    }

    public List<String> getReplicatingNodes() {
        return replicatingNodes;
    }

    public DepositorEdit setReplicatingNodes(List<String> replicatingNodes) {
        this.replicatingNodes = replicatingNodes;
        return this;
    }
}
