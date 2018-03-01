package org.chronopolis.rest.models;

import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
import java.util.List;

/**
 * For use in POST /api/depositors or creation through a form
 *
 * @author shake
 */
public class DepositorCreate {

    @NotBlank
    private String namespace;

    @NotBlank
    private String sourceOrganization;

    @NotBlank
    private String organizationAddress;

    @Valid
    private List<DepositorContactCreate> contacts;

    public String getNamespace() {
        return namespace;
    }

    public DepositorCreate setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getSourceOrganization() {
        return sourceOrganization;
    }

    public DepositorCreate setSourceOrganization(String sourceOrganization) {
        this.sourceOrganization = sourceOrganization;
        return this;
    }

    public String getOrganizationAddress() {
        return organizationAddress;
    }

    public DepositorCreate setOrganizationAddress(String organizationAddress) {
        this.organizationAddress = organizationAddress;
        return this;
    }

    public List<DepositorContactCreate> getContacts() {
        return contacts;
    }

    public DepositorCreate setContacts(List<DepositorContactCreate> contacts) {
        this.contacts = contacts;
        return this;
    }
}
