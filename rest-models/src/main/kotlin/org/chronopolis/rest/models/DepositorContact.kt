package org.chronopolis.rest.models

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
import java.util.Optional

data class DepositorContact(val contactName: String,
                            val contactEmail: String,
                            val contactPhone: String)

data class Phone(var number: String = "", var countryCode: String = "") : PhoneNumber {
    override fun number() = number
    override fun countryCode() = countryCode

    override fun formatNumber(): Optional<String> {
        val util = PhoneNumberUtil.getInstance()

        return try {
            val parsed = util.parse(number, countryCode)
            Optional.of(util.format(parsed, INTERNATIONAL))
        } catch (exception: NumberParseException) {
            Optional.empty()
        }
    }
}

