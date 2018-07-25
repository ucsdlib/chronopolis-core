package org.chronopolis.rest.kot.models

import java.time.ZonedDateTime

data class Fixity(val value: String,

                   // enum instead? we have the digest type...
                  val algorithm: String,
                  val createdAt: ZonedDateTime)

