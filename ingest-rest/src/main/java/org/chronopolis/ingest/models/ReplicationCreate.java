package org.chronopolis.ingest.models;

import java.util.ArrayList;
import java.util.List;

/**
 * This is pretty much the same as the ${link ReplicationRequest}, but with
 * a list of Nodes instead of a single
 *
 * Created by shake on 6/22/17.
 */
public class ReplicationCreate {

    private Long bag;
    private List<Long> nodes = new ArrayList<>();

    public Long getBag() {
        return bag;
    }

    public ReplicationCreate setBag(Long bag) {
        this.bag = bag;
        return this;
    }

    public List<Long> getNodes() {
        return nodes;
    }

    public ReplicationCreate setNodes(List<Long> nodes) {
        this.nodes = nodes;
        return this;
    }
}
