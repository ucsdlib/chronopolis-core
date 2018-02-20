package org.chronopolis.rest.entities;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

/**
 * yarp
 *
 * @author shake
 */
@Entity
public class DepositorContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contactName;
    private String contactPhone;
    private String contactEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depositor_id")
    private Depositor depositor;

    protected DepositorContact() {} // jpa

    public String getContactName() {
        return contactName;
    }

    public DepositorContact setContactName(String contactName) {
        this.contactName = contactName;
        return this;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public DepositorContact setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
        return this;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public DepositorContact setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
        return this;
    }

    public Depositor getDepositor() {
        return depositor;
    }

    public DepositorContact setDepositor(Depositor depositor) {
        this.depositor = depositor;
        return this;
    }
}
