package org.chronopolis.rest.entities;

import com.google.common.collect.ComparisonChain;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.Objects;

/**
 * yarp
 *
 * @author shake
 */
@Entity
public class DepositorContact implements Comparable<DepositorContact> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String contactName;
    private String contactPhone;
    private String contactEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depositor_id")
    private Depositor depositor;

    public DepositorContact() {} // jpa

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

    @Override
    public int compareTo(DepositorContact depositorContact) {
        return ComparisonChain.start()
                .compare(id, depositorContact.id) // should we use the id?
                .compare(contactName, depositorContact.contactName)
                .compare(contactEmail, depositorContact.contactEmail)
                .compare(contactPhone, depositorContact.contactPhone)
                .result();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DepositorContact that = (DepositorContact) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(contactName, that.contactName) &&
                Objects.equals(contactPhone, that.contactPhone) &&
                Objects.equals(contactEmail, that.contactEmail);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, contactName, contactPhone, contactEmail);
    }
}
