package org.chronopolis.rest.entities;

import org.chronopolis.rest.entities.fulfillment.Strategy;
import org.chronopolis.rest.models.repair.AuditStatus;
import org.chronopolis.rest.models.repair.FulfillmentType;
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
 * todo: lazy fetch where we can
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
    private Boolean validated;

    @ManyToOne
    private Bag bag;

    @ManyToOne
    @JoinColumn(name = "to_node")
    private Node to;

    @ManyToOne
    @JoinColumn(name = "from_node")
    private Node from;

    @Enumerated(value = EnumType.STRING)
    private FulfillmentType type;

    @OneToOne(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    private Strategy strategy;

    @OneToMany(mappedBy = "repair", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<RepairFile> files;


    public Repair() {
        cleaned = Boolean.FALSE;
        replaced = Boolean.FALSE;
        validated = Boolean.FALSE;
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

    public Boolean getValidated() {
        return validated;
    }

    public Repair setValidated(Boolean validated) {
        this.validated = validated;
        return this;
    }

    public Node getFrom() {
        return from;
    }

    public Repair setFrom(Node from) {
        this.from = from;
        return this;
    }

    public FulfillmentType getType() {
        return type;
    }

    public Repair setType(FulfillmentType type) {
        this.type = type;
        return this;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public Repair setStrategy(Strategy strategy) {
        this.strategy = strategy;
        return this;
    }
}
