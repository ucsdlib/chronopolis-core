package org.chronopolis.tokenize;

import com.google.common.collect.ComparisonChain;
import org.chronopolis.rest.kot.models.Bag;

import java.util.Objects;

/**
 * Any reason why this shouldn't be a kotlin data class?
 *
 * Information about an entry in a manifest.
 *
 * @author shake
 */
public class ManifestEntry implements Comparable<ManifestEntry> {

    private final Bag bag;
    private final String path;
    private final String digest;

    public ManifestEntry(Bag bag, String path, String digest) {
        this.bag = bag;
        this.path = path;
        this.digest = digest;
    }

    public Bag getBag() {
        return bag;
    }

    public String getPath() {
        return path;
    }

    public String getDigest() {
        return digest;
    }

    public String tokenName() {
        return "(" + bag.getDepositor() + "," + bag.getName() + ")::" + path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ManifestEntry that = (ManifestEntry) o;
        return Objects.equals(bag, that.bag) &&
                Objects.equals(path, that.path) &&
                Objects.equals(digest, that.digest);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bag, path, digest);
    }

    @Override
    public String toString() {
        return "ManifestEntry{" +
                "bag=" + bag +
                ", path='" + path + '\'' +
                ", digest='" + digest + '\'' +
                '}';
    }

    @Override
    public int compareTo(ManifestEntry entry) {
        return ComparisonChain.start()
                .compare(bag, entry.bag)
                .compare(path, entry.path)
                .result();
    }
}
