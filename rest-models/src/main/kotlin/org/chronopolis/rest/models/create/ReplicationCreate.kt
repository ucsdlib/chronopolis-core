package org.chronopolis.rest.models.create

import org.chronopolis.rest.models.Bag
import org.chronopolis.rest.models.Replication
import javax.validation.constraints.Min

/**
 * Request to create a [Replication]
 *
 * @property bagId the id of the [Bag] to replicate
 * @property nodeId the id of the Node to replicate to
 * @author shake
 */
data class ReplicationCreate(@get:Min(1) var bagId: Long = -1,
                             @get:Min(1) var nodeId: Long = -1)
