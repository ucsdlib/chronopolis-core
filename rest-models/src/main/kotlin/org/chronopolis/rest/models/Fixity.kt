package org.chronopolis.rest.models

import java.time.ZonedDateTime

data class Fixity(val value: String,

                   // enum instead? we have the digest type...
                  val algorithm: String,
                  val createdAt: ZonedDateTime)

