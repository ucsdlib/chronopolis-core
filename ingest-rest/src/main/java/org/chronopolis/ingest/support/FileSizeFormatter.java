package org.chronopolis.ingest.support;


import org.chronopolis.rest.models.enums.StorageUnit;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Format a size from a given Long representing the number of bytes
 * Truncate to: KiB, MiB, GiB, TiB
 *
 * @author shake
 */
public class FileSizeFormatter {

    public String format(Long number) {
        return format(new BigDecimal(number));
    }

    public String format(BigDecimal decimal) {
        Long kib = 1024L;
        StorageUnit unit = StorageUnit.B;

        BigDecimal result = decimal;
        while (result.longValue() >= kib) {
            // could totally just use 1024 and shift this nahmsayin
            result = result.divide(new BigDecimal(kib), 2, RoundingMode.HALF_UP);
            unit = unit.next();
        }

        return result.stripTrailingZeros().toPlainString() + " " + unit.name();
    }

}
