package org.chronopolis.rest.models.storage;

public class FixityCreate {

    private String algorithm;
    private String value;

    public String getAlgorithm() {
        return algorithm;
    }

    public FixityCreate setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    public String getValue() {
        return value;
    }

    public FixityCreate setValue(String value) {
        this.value = value;
        return this;
    }
}
