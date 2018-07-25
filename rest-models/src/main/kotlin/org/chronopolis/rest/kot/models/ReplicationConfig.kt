package org.chronopolis.rest.kot.models

data class ReplicationConfig(val region: Long,
                             val path: String,
                             val server: String,
                             val username: String)

