package org.chronopolis.rest.models;

import org.chronopolis.rest.constraints.E123;
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
    @NotBlank
    private String email;

    @E123
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

    /**
     * Basic encapsulation for a PhoneNumber - a national number and a country code
     *
     * Note that this class will not parse the phone number, that needs to be done externally
     */
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

        @Override
        public String toString() {
            return "PhoneNumber{" +
                    "number='" + number + '\'' +
                    ", countryCode='" + countryCode + '\'' +
                    '}';
        }
    }

}
