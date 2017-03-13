package org.chronopolis.common.ace;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * Created by shake on 3/13/17.
 */
public class CompareResponse {

    private Set<String> diff;
    private Set<String> match;
    private Set<String> notFound;

    public CompareResponse() {
        diff = new HashSet<>();
        match = new HashSet<>();
        notFound = new HashSet<>();
    }

    public Set<String> getDiff() {
        return diff;
    }

    public CompareResponse setDiff(Set<String> diff) {
        this.diff = diff;
        return this;
    }

    public Set<String> getMatch() {
        return match;
    }

    public CompareResponse setMatch(Set<String> match) {
        this.match = match;
        return this;
    }

    public Set<String> getNotFound() {
        return notFound;
    }

    public CompareResponse setNotFound(Set<String> notFound) {
        this.notFound = notFound;
        return this;
    }
}
