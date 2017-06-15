package org.chronopolis.ingest.models.filter;

import com.google.common.collect.LinkedListMultimap;
import org.chronopolis.ingest.models.Paged;
import org.chronopolis.rest.models.repair.AuditStatus;
import org.chronopolis.rest.models.repair.RepairStatus;

import java.util.List;

/**
 * Data binding for filtering on Repairs
 *
 * Created by shake on 6/15/17.
 */
public class RepairFilter extends Paged {

    private String node;
    private String fulfillingNode;
    private List<RepairStatus> status;
    private List<AuditStatus> auditStatus;

    private LinkedListMultimap<String, String> parameters = LinkedListMultimap.create();

    public String getNode() {
        return node;
    }

    public RepairFilter setNode(String node) {
        this.node = node;
        parameters.put("node", node);
        return this;
    }

    public String getFulfillingNode() {
        return fulfillingNode;
    }

    public RepairFilter setFulfillingNode(String fulfillingNode) {
        this.fulfillingNode = fulfillingNode;
        parameters.put("fulfillingNode", fulfillingNode);
        return this;
    }

    public List<RepairStatus> getStatus() {
        return status;
    }

    public RepairFilter setStatus(List<RepairStatus> status) {
        this.status = status;
        status.forEach(repairStatus -> parameters.put("status", repairStatus.name()));
        return this;
    }

    public List<AuditStatus> getAuditStatus() {
        return auditStatus;
    }

    public RepairFilter setAuditStatus(List<AuditStatus> auditStatus) {
        this.auditStatus = auditStatus;
        auditStatus.forEach(status -> parameters.put("status", status.name()));
        return this;
    }

    public LinkedListMultimap<String, String> getParameters() {
        parameters.putAll(super.getParameters());
        return parameters;
    }
}
