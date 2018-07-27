package org.chronopolis.ingest.support;

import com.google.common.collect.ImmutableList;
import org.chronopolis.rest.kot.entities.Replication;

import java.util.List;
import java.util.Optional;

/**
 * Class to encapsulate the result of creating a replication
 * Includes information about constraint satisfaction validation
 *
 * May one day be updated with generics but for now it's an experiment in.... stuff
 *
 * @author shake
 */
public class ReplicationCreateResult {

    private Replication result;
    // something other than a string would probably be better
    // but oh well deal with it
    private final List<String> errors;

    public ReplicationCreateResult(Replication result) {
        this.result = result;
        this.errors = ImmutableList.of();
    }

    public ReplicationCreateResult(List<String> errors) {
        this.errors = errors;
    }

    public Optional<Replication> getResult() {
        return Optional.ofNullable(result);
    }

    public List<String> getErrors() {
        return errors;
    }

}
