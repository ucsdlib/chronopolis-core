package org.chronopolis.rest.kot.models.enums

enum class BagStatus {
    DEPOSITED,
    INITIALIZED,
    TOKENIZED,
    REPLICATING,
    PRESERVED,
    DEPRECATED,
    DELETED,
    ERROR;

    companion object {
        fun processingStates(): Set<BagStatus> = TODO()
        fun preservedStates(): Set<BagStatus> = TODO()
        fun inactiveStates(): Set<BagStatus> = TODO()
        fun statusByGroup(): Set<BagStatus> = TODO()
    }
}