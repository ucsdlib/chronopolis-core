package org.chronopolis.rest.kot.models.create

import org.hibernate.validator.constraints.NotEmpty

data class RepairCreate(val to: String?,
                        val depositor: String,
                        val collection: String,
                        @get: NotEmpty val files: Set<String>)
