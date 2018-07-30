package org.chronopolis.rest.kot.models.enums

import com.google.common.collect.ImmutableListMultimap

enum class AuditStatus {
    PRE, AUDITING, SUCCESS, FAIL;

    companion object {
        fun statusByGroup(): ImmutableListMultimap<String, AuditStatus> =
                ImmutableListMultimap.Builder<String, AuditStatus>()
                        .put("Pending", PRE)
                        .put("Active", AUDITING)
                        .put("Success", SUCCESS)
                        .put("Failure", FAIL)
                        .build()
    }
}