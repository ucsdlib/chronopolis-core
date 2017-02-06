package org.chronopolis.rest.entities;

import org.chronopolis.rest.models.repair.RepairStatus;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * Created by shake on 11/10/16.
 */
@Entity
public class Repair extends UpdatableEntity {

    @Enumerated(value = EnumType.STRING)
    RepairStatus status;

    String requester;

    @ManyToOne
    Bag bag;

    @ManyToOne
    @JoinColumn(name = "to_node")
    Node to;

    @OneToOne(cascade = CascadeType.MERGE)
    Fulfillment fulfillment;

    @OneToMany(mappedBy = "repair", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    Set<RepairFile> files;


    public Repair() {
    }

    public RepairStatus getStatus() {
        return status;
    }

    public Repair setStatus(RepairStatus status) {
        this.status = status;
        return this;
    }

    public String getRequester() {
        return requester;
    }

    public Repair setRequester(String requester) {
        this.requester = requester;
        return this;
    }

    public Bag getBag() {
        return bag;
    }

    public Repair setBag(Bag bag) {
        this.bag = bag;
        return this;
    }

    public Fulfillment getFulfillment() {
        return fulfillment;
    }

    public Repair setFulfillment(Fulfillment fulfillment) {
        this.fulfillment = fulfillment;
        return this;
    }

    public Set<RepairFile> getFiles() {
        return files;
    }

    public Repair setFilesFromRequest(Set<String> files) {
        if (this.files == null) {
            this.files = new HashSet<>();
        }

        files.forEach(f -> this.files.add(new RepairFile().setPath(f).setRepair(this)));

        return this;
    }

    public Repair setFiles(Set<RepairFile> files) {
        this.files = files;
        return this;
    }

    public Node getTo() {
        return to;
    }

    public Repair setTo(Node to) {
        this.to = to;
        return this;
    }
}
