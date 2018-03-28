package org.chronopolis.rest.entities;

import com.google.common.collect.ComparisonChain;
import com.google.i18n.phonenumbers.NumberParseException;
import org.chronopolis.rest.models.DepositorContactCreate;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.Objects;
import java.util.Optional;

/**
 * yarp
 *
 * @author shake
 */
@Entity
public class DepositorContact extends PersistableEntity implements Comparable<DepositorContact> {

    private String contactName;
    private String contactPhone;
    private String contactEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depositor_id")
    private Depositor depositor;

    public DepositorContact() {} // jpa

    public static Optional<DepositorContact> fromCreateRequest(DepositorContactCreate create) {
        DepositorContact contact = new DepositorContact();
        Optional<DepositorContact> created = Optional.of(contact);
        contact.setContactName(create.getName());
        contact.setContactEmail(create.getEmail());
        try {
            contact.setContactPhone(create.formattedPhoneNumber());
        } catch (NumberParseException e) {
            created = Optional.empty();
        }

        return created;
    }

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
        return Objects.equals(contactName, that.contactName) &&
                Objects.equals(contactPhone, that.contactPhone) &&
                Objects.equals(contactEmail, that.contactEmail);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contactName, contactPhone, contactEmail);
    }
}
