package org.chronopolis.rest.models.create

import org.chronopolis.rest.constraints.E123
import org.chronopolis.rest.models.DepositorContact
import org.chronopolis.rest.models.Phone
import org.hibernate.validator.constraints.Email
import org.hibernate.validator.constraints.NotBlank

/**
 * Class for creating a [DepositorContact]. Has a [Phone] in order to do validation, but then treats
 * it as a string; maybe not the best idea, could probably just handle the Phone during
 * serialization.
 *
 * @author shake
 */
data class DepositorContactCreate(@get: NotBlank val contactName: String,
                                  @get:Email val contactEmail: String,
                                  @get:E123 val contactPhone: Phone)