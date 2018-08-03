package org.chronopolis.rest.models.enums

import com.google.common.collect.ImmutableListMultimap

enum class RepairStatus {
    REQUESTED, STAGING, READY, TRANSFERRED, REPAIRED, FAILED;

    companion object {
        fun statusByGroup(): ImmutableListMultimap<String, RepairStatus> =
                ImmutableListMultimap.Builder<String, RepairStatus>()
                        .put("Pending", REQUESTED)
                        .put("Active", STAGING)
                        .put("Active", READY)
                        .put("Active", TRANSFERRED)
                        .put("Success", REPAIRED)
                        .put("Failure", FAILED)
                        .build()
    }
}