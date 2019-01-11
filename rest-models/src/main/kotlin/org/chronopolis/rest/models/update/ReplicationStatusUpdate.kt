package org.chronopolis.rest.models.update

import org.chronopolis.rest.models.Replication
import org.chronopolis.rest.models.enums.ReplicationStatus

/**
 * Update a [Replication] to have a specific [ReplicationStatus]
 *
 * @property status the [ReplicationStatus] to set
 * @author shake
 */
data class ReplicationStatusUpdate(var status: ReplicationStatus = ReplicationStatus.FAILURE)