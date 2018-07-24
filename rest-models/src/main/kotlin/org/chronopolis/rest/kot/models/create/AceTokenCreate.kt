package org.chronopolis.rest.kot.models.create

import org.hibernate.validator.constraints.NotBlank
import java.time.ZonedDateTime

data class AceTokenCreate(val bagId: Long,
                          val round: Long,
                          val createDate: ZonedDateTime,
                          @get: NotBlank val proof: String,
                          @get: NotBlank val imsHost: String,
                          @get: NotBlank val filename: String,
                          @get: NotBlank val algorithm: String,
                          @get: NotBlank val imsService: String)
