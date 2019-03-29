package org.chronopolis.rest.models.create

import org.chronopolis.rest.models.Fixity
import org.chronopolis.rest.models.enums.FixityAlgorithm
import javax.validation.constraints.NotEmpty

/**
 * Request to create a [Fixity]
 *
 * @property algorithm the algorithm used
 * @property value the value the algorithm produced
 * @author shake
 */
data class FixityCreate(var algorithm: FixityAlgorithm = FixityAlgorithm.SHA_256,
                        @get:NotEmpty var value: String = "")
