package org.chronopolis.rest.entities;

import com.google.common.collect.ComparisonChain;
import org.hibernate.annotations.NaturalId;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * @author shake
 */
@Entity
public class Depositor extends UpdatableEntity implements Comparable<Depositor> {

    @NaturalId
    private String namespace;

    private String sourceOrganization;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DepositorContact> contacts = new HashSet<>();

    public Depositor() {} // JPA

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

    @Override
    public int compareTo(Depositor depositor) {
        // todo: ...reliably compare (+contacts?)
        return ComparisonChain.start()
                .compare(id, depositor.id)
                .compare(namespace, depositor.namespace)
                .compare(sourceOrganization, depositor.sourceOrganization)
                .result();
    }
}
