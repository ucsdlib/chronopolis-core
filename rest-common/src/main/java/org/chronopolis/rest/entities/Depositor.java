package org.chronopolis.rest.entities;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * @author shake
 */
@Entity
public class Depositor extends UpdatableEntity {

    private String namespace;
    private String sourceOrganization;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DepositorContact> contacts = new HashSet<>();

    protected Depositor() {} // JPA

    public String getNamespace() {
        return namespace;
    }

    public Depositor setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public String getSourceOrganization() {
        return sourceOrganization;
    }

    public Depositor setSourceOrganization(String sourceOrganization) {
        this.sourceOrganization = sourceOrganization;
        return this;
    }

    public Set<DepositorContact> getContacts() {
        return contacts;
    }
}
