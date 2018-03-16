package org.chronopolis.ingest.support;

import com.google.common.collect.ImmutableList;
import org.chronopolis.rest.entities.Bag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;

import java.util.List;
import java.util.Optional;

/**
 * Result from creating a Bag
 *
 * @author shake
 */
public class BagCreateResult {

    private Bag bag;

    private final Status status;
    // Not really used atm, maybe laters
    private final ImmutableList<String> errors;

    public BagCreateResult(Bag bag) {
        this.bag = bag;
        this.errors = ImmutableList.of();
        this.status = Status.CREATED;
    }

    public BagCreateResult(List<String> errors, Status status) {
        this.errors = ImmutableList.copyOf(errors);
        this.status = status;
    }

    public Optional<Bag> getBag() {
        return Optional.ofNullable(bag);
    }

    public ResponseEntity<Bag> getResponseEntity() {
        if (bag == null) {
            return status.builder.build();
        } else {
            return status.builder.body(bag);
        }
    }

    public enum Status {
        CREATED(ResponseEntity.status(HttpStatus.CREATED)),
        CONFLICT(ResponseEntity.status(HttpStatus.CONFLICT)),
        BAD_REQUEST(ResponseEntity.badRequest());

        private final BodyBuilder builder;

        Status(BodyBuilder builder) {
            this.builder = builder;
        }
    }
}
