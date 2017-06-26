package org.chronopolis.common.ace;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * Created by shake on 3/13/17.
 */
public class CompareRequest {

    private List<CompareFile> comparisons;

    public CompareRequest() {
        comparisons = new ArrayList<>();
    }

    public List<CompareFile> getComparisons() {
        return comparisons;
    }

    public CompareRequest addComparison(CompareFile file) {
        if (comparisons == null) {
            comparisons = new ArrayList<>();
        }

        comparisons.add(file);
        return this;
    }

    public CompareRequest setComparisons(List<CompareFile> comparisons) {
        this.comparisons = comparisons;
        return this;
    }
}
