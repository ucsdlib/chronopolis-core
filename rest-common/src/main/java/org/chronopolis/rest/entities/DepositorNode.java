package org.chronopolis.rest.entities;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Entity representing a join table for Depositor/Nodes
 *
 * @author shake
 */
@Entity
@Table(name = "depositor_distribution")
public class DepositorNode extends PersistableEntity implements Serializable {

    @ManyToOne
    private Depositor depositor;

    @ManyToOne
    private Node node;

    public DepositorNode() {
    }

    public DepositorNode(Depositor depositor, Node node) {
        this.depositor = depositor;
        this.node = node;
    }

    public Depositor getDepositor() {
        return depositor;
    }

    public DepositorNode setDepositor(Depositor depositor) {
        this.depositor = depositor;
        return this;
    }

    public Node getNode() {
        return node;
    }

    public DepositorNode setNode(Node node) {
        this.node = node;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepositorNode that = (DepositorNode) o;
        return Objects.equals(depositor, that.depositor) &&
                Objects.equals(node, that.node);
    }

    @Override
    public int hashCode() {
        return Objects.hash(depositor, node);
    }

    @Override
    public String toString() {
        return "DepositorNode{" +
                "depositor=" + (depositor == null ? "null" : depositor.getNamespace()) +
                ", node=" + (node == null? "null" : node.username) +
                '}';
    }
}
