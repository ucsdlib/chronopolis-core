package org.chronopolis.rest.models.create

import org.chronopolis.rest.models.Fixity
import org.hibernate.validator.constraints.NotEmpty

/**
 * Request to create a [Fixity]
 *
 * @property algorithm the algorithm used
 * @property value the value the algorithm produced
 * @author shake
 */
data class FixityCreate(@get:NotEmpty var algorithm: String = "",
                        @get:NotEmpty var value: String = "")
