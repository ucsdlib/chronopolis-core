package org.chronopolis.rest.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Class to keep track of where bags are distributed to
 *
 * Created by shake on 7/17/15.
 */
@Entity
public class BagDistribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private Bag bag;

    @ManyToOne(fetch = FetchType.EAGER)
    private Node node;

    @Enumerated(EnumType.STRING)
    private BagDistributionStatus status;

    public Long getId() {
        return id;
    }

    public Bag getBag() {
        return bag;
    }

    public BagDistribution setBag(Bag bag) {
        this.bag = bag;
        return this;
    }

    public Node getNode() {
        return node;
    }

    public BagDistribution setNode(Node node) {
        this.node = node;
        return this;
    }

    public BagDistributionStatus getStatus() {
        return status;
    }

    public BagDistribution setStatus(BagDistributionStatus status) {
        this.status = status;
        return this;
    }

    public enum BagDistributionStatus {
        DISTRIBUTE, REPLICATE
    }

}
