package org.chronopolis.rest.models.create

import org.hibernate.validator.constraints.NotBlank
import javax.validation.Valid

data class DepositorCreate(@get:NotBlank val namespace: String,
                           @get:NotBlank val sourceOrganization: String,
                           @get:NotBlank val organizationAddress: String,
                           @get:Valid val contacts: List<DepositorContactCreate>,
                           val replicatingNodes: List<String>)