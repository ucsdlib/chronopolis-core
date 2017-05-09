package org.chronopolis.rest.models.repair;

import com.google.common.collect.ComparisonChain;

import java.time.ZonedDateTime;

/**
 * Fulfillment of a repair
 *
 * Created by shake on 11/10/16.
 */
@Deprecated
public class Fulfillment implements Comparable<Fulfillment> {

    private Long id;
    private Long repair;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private String to;
    private String from;
    private boolean cleaned;
    private boolean validated;
    private FulfillmentType type;
    private FulfillmentStatus status;
    private FulfillmentStrategy credentials;

    public Long getId() {
        return id;
    }

    public Fulfillment setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getRepair() {
        return repair;
    }

    public Fulfillment setRepair(Long repair) {
        this.repair = repair;
        return this;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public Fulfillment setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Fulfillment setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public String getTo() {
        return to;
    }

    public Fulfillment setTo(String to) {
        this.to = to;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public Fulfillment setFrom(String from) {
        this.from = from;
        return this;
    }

    public FulfillmentType getType() {
        return type;
    }

    public Fulfillment setType(FulfillmentType type) {
        this.type = type;
        return this;
    }

    public FulfillmentStatus getStatus() {
        return status;
    }

    public Fulfillment setStatus(FulfillmentStatus status) {
        this.status = status;
        return this;
    }

    public FulfillmentStrategy getCredentials() {
        return credentials;
    }

    public Fulfillment setCredentials(FulfillmentStrategy credentials) {
        this.credentials = credentials;
        return this;
    }

    public boolean isCleaned() {
        return cleaned;
    }

    public Fulfillment setCleaned(boolean cleaned) {
        this.cleaned = cleaned;
        return this;
    }

    public boolean isValidated() {
        return validated;
    }

    public Fulfillment setValidated(boolean validated) {
        this.validated = validated;
        return this;
    }

    @Override
    public int compareTo(Fulfillment fulfillment) {
        return ComparisonChain.start()
                .compare(id, fulfillment.id)
                .compare(to, fulfillment.to)
                .compare(from, fulfillment.from)
                .compare(repair, fulfillment.repair)
                .result();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Fulfillment that = (Fulfillment) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (repair != null ? !repair.equals(that.repair) : that.repair != null) return false;
        if (to != null ? !to.equals(that.to) : that.to != null) return false;
        return from != null ? from.equals(that.from) : that.from == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (repair != null ? repair.hashCode() : 0);
        result = 31 * result + (to != null ? to.hashCode() : 0);
        result = 31 * result + (from != null ? from.hashCode() : 0);
        return result;
    }
}
