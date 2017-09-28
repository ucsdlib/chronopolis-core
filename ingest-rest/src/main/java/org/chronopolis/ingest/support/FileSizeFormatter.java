package org.chronopolis.ingest.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Format a size from a given Long representing the number of bytes
 * Truncate to: KiB, MiB, GiB, TiB
 *
 * @author shake
 */
public class FileSizeFormatter {

    public String format(BigDecimal decimal) {
        Unit unit = Unit.B;

        BigDecimal result = decimal;
        while (result.longValue() >= 1000L) {
            // could totally just use 1024 and shift this nahmsayin
            result = result.divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);
            unit = unit.next();
        }

        return result.stripTrailingZeros().toPlainString() + " " + unit.name();
    }

    public enum Unit {
        B, KiB, MiB, GiB, TiB, OOB;

        public Unit next() {
            switch (this) {
                case B:
                    return KiB;
                case KiB:
                    return MiB;
                case MiB:
                    return GiB;
                case GiB:
                    return TiB;
                case TiB:
                    return OOB;
                case OOB:
                    return OOB;
                default:
                    return OOB;
            }
        }
    }

}
