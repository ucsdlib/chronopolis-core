package org.chronopolis.rest.models.storage;

import java.time.ZonedDateTime;

/**
 * Model for fixity entity
 *
 * Created by shake on 7/14/17.
 */
public class Fixity {

    private String value;

    // enum instead? we have the digest type...
    private String algorithm;
    private ZonedDateTime createdAt;

    public Fixity(String algorithm, String value, ZonedDateTime createdAt) {
        this.value = value;
        this.algorithm = algorithm;
        this.createdAt = createdAt;
    }

    public String getValue() {
        return value;
    }

    public Fixity setValue(String value) {
        this.value = value;
        return this;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public Fixity setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public Fixity setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }
}
