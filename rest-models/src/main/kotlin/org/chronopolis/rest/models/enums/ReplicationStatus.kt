package org.chronopolis.rest.models.enums

import com.google.common.collect.ImmutableListMultimap
import com.google.common.collect.ImmutableSet

enum class ReplicationStatus {
    PENDING,
    STARTED,
    TRANSFERRED,
    SUCCESS,
    ACE_REGISTERED,
    ACE_TOKEN_LOADED,
    ACE_AUDITING,

    FAILURE_ACE_AUDIT,
    FAILURE_TOKEN_STORE,
    FAILURE_TAG_MANIFEST,
    FAILURE;

    fun isFailure(): Boolean = this == FAILURE
            || this == FAILURE_ACE_AUDIT
            || this == FAILURE_TOKEN_STORE
            || this == FAILURE_TAG_MANIFEST

    fun isClientStatus(): Boolean = this == STARTED
            || this == SUCCESS
            || this == FAILURE
            || this == ACE_AUDITING
            || this == ACE_TOKEN_LOADED
            || this == ACE_REGISTERED

    fun isOngoing(): Boolean = !(this.isFailure() || this == SUCCESS)

    companion object {
        fun active(): Set<ReplicationStatus> = ImmutableSet.of(PENDING, STARTED, TRANSFERRED,
                ACE_REGISTERED, ACE_AUDITING, ACE_TOKEN_LOADED)

        fun statusByGroup(): ImmutableListMultimap<String, ReplicationStatus> =
                ImmutableListMultimap.Builder<String, ReplicationStatus>()
                        .put("Inactive", PENDING)
                        .putAll("Active", active())
                        .put("Success", SUCCESS)
                        .put("Failed", FAILURE)
                        .put("Failed", FAILURE_ACE_AUDIT)
                        .put("Failed", FAILURE_TAG_MANIFEST)
                        .put("Failed", FAILURE_TOKEN_STORE)
                        .build()
    }

}