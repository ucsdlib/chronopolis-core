package org.chronopolis.rest.kot.models.enums

enum class RepairStatus {
    REQUESTED, STAGING, READY, TRANSFERRED, REPAIRED, FAILED;

    companion object {
        fun statusByGroup(): Set<RepairStatus> = TODO()
    }
}