package org.chronopolis.rest.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.util.Set;

/**
 * API Model for a Depositor in Chronopolis
 *
 * @author shake
 */
public class DepositorModel {

    private final Long id;
    private final String namespace;
    private final String sourceOrganization;
    private final String organizationAddress;
    private final ZonedDateTime createdAt;
    private final ZonedDateTime updatedAt;
    private final Set<DepositorContactModel> contacts;

    @JsonCreator
    public DepositorModel(@JsonProperty("id") Long id,
                          @JsonProperty("namespace") String namespace,
                          @JsonProperty("sourceOrganization") String sourceOrganization,
                          @JsonProperty("organizationAddress") String organizationAddress,
                          @JsonProperty("createdAt") ZonedDateTime createdAt,
                          @JsonProperty("updatedAt") ZonedDateTime updatedAt,
                          @JsonProperty("contacts") Set<DepositorContactModel> contacts) {
        this.id = id;
        this.namespace = namespace;
        this.sourceOrganization = sourceOrganization;
        this.organizationAddress = organizationAddress;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.contacts = contacts;
    }

    public Long getId() {
        return id;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getSourceOrganization() {
        return sourceOrganization;
    }

    public String getOrganizationAddress() {
        return organizationAddress;
    }

    public Set<DepositorContactModel> getContacts() {
        return contacts;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }
}
