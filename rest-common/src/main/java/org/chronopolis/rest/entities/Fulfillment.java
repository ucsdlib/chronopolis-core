package org.chronopolis.rest.entities;

import org.chronopolis.rest.entities.fulfillment.Strategy;
import org.chronopolis.rest.models.repair.FulfillmentStatus;
import org.chronopolis.rest.models.repair.FulfillmentType;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToOne;

/**
 *
 * Created by shake on 11/10/16.
 */
@Entity
public class Fulfillment extends UpdatableEntity {

    @OneToOne
    Repair repair;

    @Column(name = "from_name")
    String from;

    @Enumerated(value = EnumType.STRING)
    FulfillmentStatus status;

    @Enumerated(value = EnumType.STRING)
    FulfillmentType type;

    @OneToOne
    Strategy strategy;

    public Fulfillment() {
    }

    public Repair getRepair() {
        return repair;
    }

    public Fulfillment setRepair(Repair repair) {
        this.repair = repair;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public Fulfillment setFrom(String from) {
        this.from = from;
        return this;
    }

    public FulfillmentStatus getStatus() {
        return status;
    }

    public Fulfillment setStatus(FulfillmentStatus status) {
        this.status = status;
        return this;
    }

    public FulfillmentType getType() {
        return type;
    }

    public Fulfillment setType(FulfillmentType type) {
        this.type = type;
        return this;
    }

    public Strategy getStrategy() {
        return strategy;
    }

    public Fulfillment setStrategy(Strategy strategy) {
        this.strategy = strategy;
        return this;
    }
}
