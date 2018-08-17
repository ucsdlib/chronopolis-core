package org.chronopolis.rest.models

import java.util.Optional

interface PhoneNumber {
    fun number(): String
    fun countryCode(): String

    fun formatNumber(): Optional<String>
}