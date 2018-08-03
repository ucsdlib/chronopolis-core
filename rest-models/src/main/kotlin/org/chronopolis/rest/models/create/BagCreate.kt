package org.chronopolis.rest.models.create

data class BagCreate(val name: String,
                     val size: Long,
                     val totalFiles: Long,
                     val storageRegion: Long,
                     val location: String,
                     val depositor: String)
