package org.chronopolis.rest.constraints

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import org.chronopolis.rest.models.PhoneNumber
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

/**
 * Validator for a phone number using libphonenumber
 *
 * @author shake
 */
class PhoneNumberValidator : ConstraintValidator<E123, PhoneNumber> {
    override fun initialize(constraintAnnotation: E123) {
    }

    override fun isValid(number: PhoneNumber, context: ConstraintValidatorContext): Boolean {
        val util = PhoneNumberUtil.getInstance()

        return try {
            val parsed = util.parse(number.number(), number.countryCode())
            util.isValidNumber(parsed)
        } catch (exception: NumberParseException) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(exception.message)
                    .addConstraintViolation()
            false
        }
    }
}