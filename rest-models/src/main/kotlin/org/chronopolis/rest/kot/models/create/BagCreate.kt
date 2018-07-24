package org.chronopolis.rest.kot.models.create

data class BagCreate(val name: String,
                     val size: Long,
                     val totalFiles: Long,
                     val storageRegion: Long,
                     val location: String,
                     val depositor: String)
