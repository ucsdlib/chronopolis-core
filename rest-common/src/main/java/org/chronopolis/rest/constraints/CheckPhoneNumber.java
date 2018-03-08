package org.chronopolis.rest.constraints;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.chronopolis.rest.models.DepositorContactCreate;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator for PhoneNumbers using libphonenumber
 *
 * @author shake
 */
public class CheckPhoneNumber implements
        ConstraintValidator<E123, DepositorContactCreate.PhoneNumber> {

    @Override
    public void initialize(E123 constraint) {
    }

    @Override
    public boolean isValid(DepositorContactCreate.PhoneNumber number,
                           ConstraintValidatorContext context) {
        if (number == null || number.getNumber() == null || number.getCountryCode() == null) {
            return false;
        }

        boolean valid;
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            Phonenumber.PhoneNumber parsed = phoneUtil.parse(number.getNumber(),
                    number.getCountryCode());
            valid = phoneUtil.isValidNumber(parsed);
        } catch (NumberParseException e) {
            valid = false;
        }
        return valid;
    }
}
