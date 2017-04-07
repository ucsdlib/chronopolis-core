package org.chronopolis.rest.entities;

import org.chronopolis.rest.models.repair.AuditStatus;
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
    private RepairStatus status;

    @Enumerated(value = EnumType.STRING)
    private AuditStatus audit;

    private String requester;
    private Boolean cleaned;
    private Boolean replaced;

    @ManyToOne
    private Bag bag;

    @ManyToOne
    @JoinColumn(name = "to_node")
    private Node to;

    @OneToOne(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinColumn(name = "fulfillment_id")
    private Fulfillment fulfillment;

    @OneToMany(mappedBy = "repair", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<RepairFile> files;


    public Repair() {
        cleaned = false;
        replaced = false;
        audit = AuditStatus.PRE;
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

    public AuditStatus getAudit() {
        return audit;
    }

    public Repair setAudit(AuditStatus audit) {
        this.audit = audit;
        return this;
    }

    public Boolean getCleaned() {
        return cleaned;
    }

    public Repair setCleaned(Boolean cleaned) {
        this.cleaned = cleaned;
        return this;
    }

    public Boolean getReplaced() {
        return replaced;
    }

    public Repair setReplaced(Boolean replaced) {
        this.replaced = replaced;
        return this;
    }
}
