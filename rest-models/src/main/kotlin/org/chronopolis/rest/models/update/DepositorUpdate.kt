package org.chronopolis.rest.models.update

import org.chronopolis.rest.models.Depositor

/**
 * Possible updates to a [Depositor]
 *
 * @property sourceOrganization the organizational name of the [Depositor]
 * @property organizationAddress an address for the [Depositor]
 * @property replicatingNodes an initial list of nodes, identified by their name, which this
 * @author shake
 */
data class DepositorUpdate(var sourceOrganization: String = "",
                           var organizationAddress: String = "",
                           var replicatingNodes: List<String> = mutableListOf())
