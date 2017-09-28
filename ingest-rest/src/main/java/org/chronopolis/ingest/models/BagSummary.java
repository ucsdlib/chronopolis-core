package org.chronopolis.ingest.models;

import org.chronopolis.ingest.support.FileSizeFormatter;
import org.chronopolis.rest.models.BagStatus;

import java.math.BigDecimal;

/**
 * UI Model to encapsulate some information about Bags
 *
 * @author shake
 */
public class BagSummary {

    private final Long sum;
    private final Long count;
    private final BagStatus status;
    private final FileSizeFormatter formatter = new FileSizeFormatter();

    public BagSummary(Long sum, Long count, BagStatus status) {
        this.sum = sum;
        this.count = count;
        this.status = status;
    }

    public String getFormattedSum() {
        return formatter.format(new BigDecimal(sum));
    }

    public Long getSum() {
        return sum;
    }

    public Long getCount() {
        return count;
    }

    public BagStatus getStatus() {
        return status;
    }
}
