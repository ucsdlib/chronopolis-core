package org.chronopolis.rest.kot.models.update

data class DepositorUpdate(val sourceOrganization: String,
                           val organizationAddress: String,
                           val replicatingNodes: List<String>)
