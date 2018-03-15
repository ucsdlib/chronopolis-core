package org.chronopolis.rest.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model for Depositor Contact Information
 *
 * @author shake
 */
public class DepositorContactModel {

    private final String contactName;
    private final String contactEmail;
    private final String contactPhone;

    @JsonCreator
    public DepositorContactModel(@JsonProperty("contactName") String contactName,
                                 @JsonProperty("contactEmail") String contactEmail,
                                 @JsonProperty("contactPhone") String contactPhone) {
        this.contactName = contactName;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }
}
