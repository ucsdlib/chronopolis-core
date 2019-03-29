package org.chronopolis.rest.models.create

import org.chronopolis.rest.constraints.E123
import org.chronopolis.rest.models.DepositorContact
import org.chronopolis.rest.models.Phone
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

/**
 * Class for creating a [DepositorContact]. Has a [Phone] in order to do validation, but then treats
 * it as a string; maybe not the best idea, could probably just handle the Phone during
 * serialization.
 *
 * @property contactName The name of the contact
 * @property contactEmail The email of the contact
 * @property contactPhone The phone number of the contact
 * @author shake
 */
data class DepositorContactCreate(@get:NotBlank var contactName: String = "",
                                  @get:Email var contactEmail: String = "",
                                  @get:E123 var contactPhone: Phone = Phone())