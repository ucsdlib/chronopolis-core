package org.chronopolis.rest.support;

public enum StorageUnit {
    B(0), KiB(1), MiB(2), GiB(3), TiB(4), OOB(-1);

    private final int power;

    StorageUnit(int power) {
        this.power = power;
    }

    public StorageUnit next() {
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

    public int getPower() {
        return power;
    }
}
