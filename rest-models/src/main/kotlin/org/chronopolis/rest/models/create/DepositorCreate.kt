package org.chronopolis.rest.models.create

import org.chronopolis.rest.models.Depositor
import org.chronopolis.rest.models.DepositorContact
import javax.validation.constraints.NotBlank
import javax.validation.Valid

/**
 * Request used to create a [Depositor]
 *
 * @property namespace the namespace to use for the [Depositor]
 * @property sourceOrganization the organizational name of the [Depositor]
 * @property organizationAddress an address for the [Depositor]
 * @property contacts an initial list of [DepositorContact]s
 * @property replicatingNodes an initial list of nodes, identified by their name, which this
 * [Depositor] will distribute data to
 * @author shake
 */
data class DepositorCreate(@get:NotBlank var namespace: String = "",
                           @get:NotBlank var sourceOrganization: String = "",
                           @get:NotBlank var organizationAddress: String = "",
                           @get:Valid var contacts: List<DepositorContactCreate> = mutableListOf(),
                           var replicatingNodes: List<String> = mutableListOf())