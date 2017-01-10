package org.chronopolis.rest.entities.fulfillment;

import org.chronopolis.rest.entities.Fulfillment;

import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
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
    private Long id;

    @OneToOne
    private Fulfillment fulfillment;

    public Long getId() {
        return id;
    }

    public Strategy setId(Long id) {
        this.id = id;
        return this;
    }

    public Fulfillment getFulfillment() {
        return fulfillment;
    }

    public Strategy setFulfillment(Fulfillment fulfillment) {
        this.fulfillment = fulfillment;
        return this;
    }
}
