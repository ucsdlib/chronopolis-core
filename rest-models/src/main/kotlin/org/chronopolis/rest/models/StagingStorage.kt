package org.chronopolis.rest.models

data class StagingStorage(val active: Boolean,
                          val size: Long,
                          val region: Long,
                          val totalFiles: Long,
                          val path: String,
                          val fixities: Set<Fixity>)

