package org.chronopolis.tokenize;

import org.chronopolis.rest.models.Bag;

import java.util.Objects;

/**
 * Information about an entry in a manifest.
 *
 * @author shake
 */
public class ManifestEntry implements Validatable {

    private final Bag bag;
    private final String path;
    private final String registeredDigest;

    private String calculatedDigest;

    public ManifestEntry(Bag bag, String path, String registeredDigest) {
        this.bag = bag;
        this.path = path;
        this.registeredDigest = registeredDigest;
    }

    public String getCalculatedDigest() {
        return calculatedDigest;
    }

    public ManifestEntry setCalculatedDigest(String calculatedDigest) {
        this.calculatedDigest = calculatedDigest;
        return this;
    }

    public Bag getBag() {
        return bag;
    }

    public String getPath() {
        return path;
    }

    public String getRegisteredDigest() {
        return registeredDigest;
    }

    public String tokenName() {
        return "(" + bag.getDepositor() + "," + bag.getName() + ")::" + path;
    }

    @Override
    public boolean isValid() {
        return Objects.equals(registeredDigest, calculatedDigest);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManifestEntry that = (ManifestEntry) o;
        return Objects.equals(bag, that.bag) &&
                Objects.equals(path, that.path) &&
                Objects.equals(registeredDigest, that.registeredDigest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bag, path, registeredDigest);
    }

    @Override
    public String toString() {
        return "ManifestEntry{" +
                "bag=" + bag +
                ", path='" + path + '\'' +
                ", registeredDigest='" + registeredDigest + '\'' +
                '}';
    }
}
