package org.chronopolis.rest.models.repair;

import com.google.common.collect.ImmutableListMultimap;

/**
 * Encapsulate some states for Repair audits
 *
 * Created by shake on 2/23/17.
 */
public enum AuditStatus {
    PRE, AUDITING, SUCCESS, FAIL;

    public static ImmutableListMultimap<String, AuditStatus> statusByGroup() {
        return new ImmutableListMultimap.Builder<String, AuditStatus>()
                .put("Pending", AuditStatus.PRE)
                .put("Active", AuditStatus.AUDITING)
                .put("Success", AuditStatus.SUCCESS)
                .put("Failure", AuditStatus.FAIL)
                .build();
    }
}
