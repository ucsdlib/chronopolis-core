package org.chronopolis.rest.models.create

import org.hibernate.validator.constraints.Email
import org.hibernate.validator.constraints.NotBlank

/**
 * Is this needed? Or can we just use the [DepositorContact]
 *
 */
data class DepositorContactCreate(@get:NotBlank val contactName: String,
                                  @get:Email val contactEmail: String,
                                  @get:NotBlank val contactPhone: String)