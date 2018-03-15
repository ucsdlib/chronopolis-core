package org.chronopolis.rest.entities;

import com.google.common.collect.ComparisonChain;
import org.hibernate.annotations.NaturalId;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author shake
 */
@Entity
public class Depositor extends UpdatableEntity implements Comparable<Depositor> {

    @NaturalId
    private String namespace;

    private String sourceOrganization;
    private String organizationAddress;

    @OneToMany(mappedBy = "depositor", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DepositorContact> contacts = new HashSet<>();

    @OneToMany(mappedBy = "depositor", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<DepositorNode> nodeDistributions = new HashSet<>();

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

    public String getOrganizationAddress() {
        return organizationAddress;
    }

    public Depositor setOrganizationAddress(String organizationAddress) {
        this.organizationAddress = organizationAddress;
        return this;
    }

    public Depositor setSourceOrganization(String sourceOrganization) {
        this.sourceOrganization = sourceOrganization;
        return this;
    }

    public Depositor addContact(DepositorContact contact) {
        contacts.add(contact);
        contact.setDepositor(this);
        return this;
    }

    public void removeContact(DepositorContact contact) {
        contacts.remove(contact);
        contact.setDepositor(null);
    }

    public Set<DepositorContact> getContacts() {
        return contacts;
    }

    public Depositor setContacts(Set<DepositorContact> contacts) {
        this.contacts = contacts;
        return this;
    }

    public Set<DepositorNode> getNodeDistributions() {
        return nodeDistributions;
    }

    public void addNodeDistribution(Node node) {
        DepositorNode depositorNode = new DepositorNode(this, node);
        nodeDistributions.add(depositorNode);
        node.getDepositorDistributions().add(depositorNode);
    }

    public void removeNodeDistribution(Node node) {
        DepositorNode depositorNode = new DepositorNode(this, node);
        nodeDistributions.remove(depositorNode);
        node.getDepositorDistributions().remove(depositorNode);
        depositorNode.setNode(null);
        depositorNode.setDepositor(null);
    }

    @Override
    public int compareTo(Depositor depositor) {
        return ComparisonChain.start()
                .compare(namespace, depositor.namespace)
                .compare(sourceOrganization, depositor.sourceOrganization)
                .compare(organizationAddress, depositor.organizationAddress)
                .result();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Depositor depositor = (Depositor) o;
        return Objects.equals(namespace, depositor.namespace) &&
                Objects.equals(sourceOrganization, depositor.sourceOrganization) &&
                Objects.equals(organizationAddress, depositor.organizationAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespace, sourceOrganization, organizationAddress);
    }
}
