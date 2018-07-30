package org.chronopolis.rest.kot.models.enums

import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableSet

/**
 * Status types for Bags
 *
 * @author shake
 */
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
        fun processingStates(): Set<BagStatus> =
                ImmutableSet.of(DEPOSITED, INITIALIZED, TOKENIZED, REPLICATING)

        fun preservedStates(): Set<BagStatus> = ImmutableSet.of(PRESERVED)

        fun inactiveStates(): Set<BagStatus> = ImmutableSet.of(DEPRECATED, DELETED, ERROR)

        fun statusByGroup(): ImmutableListMultimap<String, BagStatus> =
                ImmutableListMultimap.Builder<String, BagStatus>()
                        .putAll("Processing", processingStates())
                        .putAll("Preserved", preservedStates())
                        .putAll("Inactive", inactiveStates())
                        .build()
    }
}