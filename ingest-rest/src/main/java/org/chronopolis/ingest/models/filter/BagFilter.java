package org.chronopolis.ingest.models.filter;

import com.google.common.collect.LinkedListMultimap;
import org.chronopolis.ingest.models.Paged;
import org.chronopolis.rest.models.BagStatus;

import java.util.List;

/**
 * Data binding for query params when filtering on Bags
 *
 * Created by shake on 6/15/17.
 */
public class BagFilter extends Paged {

    private String name;
    private String depositor;
    private List<BagStatus> status;

    private LinkedListMultimap<String, String> parameters = LinkedListMultimap.create();

    public String getName() {
        return name;
    }

    public BagFilter setName(String name) {
        this.name = name;
        parameters.put("name", name);
        return this;
    }

    public String getDepositor() {
        return depositor;
    }

    public BagFilter setDepositor(String depositor) {
        this.depositor = depositor;
        parameters.put("depositor", depositor);
        return this;
    }

    public List<BagStatus> getStatus() {
        return status;
    }

    public BagFilter setStatus(List<BagStatus> status) {
        this.status = status;
        status.forEach(bagStatus -> parameters.put("status", bagStatus.name()));
        return this;
    }

    public LinkedListMultimap<String, String> getParameters() {
        parameters.putAll(super.getParameters());
        return parameters;
    }
}
