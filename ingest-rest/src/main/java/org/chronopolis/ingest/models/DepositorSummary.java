package org.chronopolis.ingest.models;

/**
 * Summary of Bags which a depositor holds
 *
 * @author shake
 */
public class DepositorSummary {

    private final Long sum;
    private final Long count;
    private final String depositor;

    public DepositorSummary(Long sum, Long count, String depositor) {
        this.sum = sum;
        this.count = count;
        this.depositor = depositor;
    }

    public Long getSum() {
        return sum;
    }

    public Long getCount() {
        return count;
    }

    public String getDepositor() {
        return depositor;
    }
}
