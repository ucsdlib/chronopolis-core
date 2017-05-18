package org.chronopolis.rest.entities.fulfillment;

import org.chronopolis.rest.entities.Repair;
import org.chronopolis.rest.models.repair.FulfillmentStrategy;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * Abstract class representing a fulfillment strategy
 *
 * Created by shake on 11/11/16.
 */
@Entity
@Inheritance
@DiscriminatorColumn(name = "TYPE")
@Table(name = "strategy")
public abstract class Strategy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(mappedBy = "strategy")
    private Repair repair;

    public Long getId() {
        return id;
    }

    public Strategy setId(Long id) {
        this.id = id;
        return this;
    }

    public Repair getRepair() {
        return repair;
    }

    public Strategy setRepair(Repair repair) {
        this.repair = repair;
        return this;
    }

    public abstract FulfillmentStrategy createModel();

}
