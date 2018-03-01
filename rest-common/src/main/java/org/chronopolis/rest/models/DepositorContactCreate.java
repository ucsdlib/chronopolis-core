package org.chronopolis.rest.models;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

/**
 * Depositor Contact fields + validation
 *
 * @author shake
 */
public class DepositorContactCreate {

    @NotBlank
    private String name;

    @Email
    private String email;

    // @E123 // Removed while this is still being developed
    private PhoneNumber phoneNumber;

    public String getName() {
        return name;
    }

    public DepositorContactCreate setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public DepositorContactCreate setEmail(String email) {
        this.email = email;
        return this;
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    public DepositorContactCreate setPhoneNumber(PhoneNumber phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public static class PhoneNumber {
        private String number;
        private String countryCode;

        public String getNumber() {
            return number;
        }

        public PhoneNumber setNumber(String number) {
            this.number = number;
            return this;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public PhoneNumber setCountryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }
    }

}
