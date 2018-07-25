package org.chronopolis.rest.kot.models.enums

enum class AuditStatus {
    PRE, AUDITING, SUCCESS, FAIL;

    companion object {
        fun statusByGroup(): Set<AuditStatus> = TODO()
    }
}