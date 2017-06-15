package org.chronopolis.ingest.models.filter;

import com.google.common.collect.LinkedListMultimap;
import org.chronopolis.ingest.models.Paged;
import org.chronopolis.rest.models.ReplicationStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Model for binding data when filtering on replications
 *
 * Created by shake on 6/15/17.
 */
public class ReplicationFilter extends Paged {

    private String node;
    private String bag;
    private List<ReplicationStatus> status = new ArrayList<>();

    private LinkedListMultimap<String, String> parameters = LinkedListMultimap.create();

    public String getNode() {
        return node;
    }

    public ReplicationFilter setNode(String node) {
        this.node = node;
        parameters.put("node", node);
        return this;
    }

    public String getBag() {
        return bag;
    }

    public ReplicationFilter setBag(String bag) {
        this.bag = bag;
        parameters.put("bag", bag);
        return this;
    }

    public List<ReplicationStatus> getStatus() {
        return status;
    }

    public ReplicationFilter setStatus(List<ReplicationStatus> status) {
        this.status = status;
        status.forEach(replicationStatus -> parameters.put("stauts", replicationStatus.name()));
        return this;
    }

    public LinkedListMultimap<String, String> getParameters() {
        parameters.putAll(super.getParameters());
        return parameters;
    }
}
