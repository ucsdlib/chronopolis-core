package org.chronopolis.rest.models.repair;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * REST model for our Repair
 *
 * Created by shake on 11/10/16.
 */
public class Repair {

    private Long id;
    private Long fulfillment;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private boolean cleaned;
    private boolean replaced;
    private AuditStatus audit;
    private RepairStatus status;
    private String to;
    private String requester;
    private String depositor;
    private String collection;
    private List<String> files;

    public Long getId() {
        return id;
    }

    public Repair setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getFulfillment() {
        return fulfillment;
    }

    public Repair setFulfillment(Long fulfillment) {
        this.fulfillment = fulfillment;
        return this;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public Repair setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Repair setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
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

    public String getDepositor() {
        return depositor;
    }

    public Repair setDepositor(String depositor) {
        this.depositor = depositor;
        return this;
    }

    public String getCollection() {
        return collection;
    }

    public Repair setCollection(String collection) {
        this.collection = collection;
        return this;
    }

    public List<String> getFiles() {
        return files;
    }

    public Repair setFiles(List<String> files) {
        this.files = files;
        return this;
    }

    public String getTo() {
        return to;
    }

    public Repair setTo(String to) {
        this.to = to;
        return this;
    }

    public boolean isCleaned() {
        return cleaned;
    }

    public Repair setCleaned(boolean cleaned) {
        this.cleaned = cleaned;
        return this;
    }

    public boolean isReplaced() {
        return replaced;
    }

    public Repair setReplaced(boolean replaced) {
        this.replaced = replaced;
        return this;
    }

    public AuditStatus getAudit() {
        return audit;
    }

    public Repair setAudit(AuditStatus audit) {
        this.audit = audit;
        return this;
    }
}
