package org.chronopolis.rest.models;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.chronopolis.rest.constraints.E123;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotBlank;

import static com.google.i18n.phonenumbers.NumberParseException.ErrorType.NOT_A_NUMBER;

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

    public String formattedPhoneNumber() throws NumberParseException {
        String number;
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        if (phoneNumber != null &&
                phoneNumber.number != null &&
                phoneNumber.countryCode != null) {
            Phonenumber.PhoneNumber parsed =
                    util.parse(phoneNumber.number, phoneNumber.countryCode);
            number = util.format(parsed, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
        } else {
            throw new NumberParseException(NOT_A_NUMBER, "Null PhoneNumber");
        }

        return number;
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
