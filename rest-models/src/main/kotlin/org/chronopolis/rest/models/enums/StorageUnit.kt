package org.chronopolis.rest.models.enums

enum class StorageUnit(val power: Int) {
    B(0), KiB(1), MiB(2), GiB(3), TiB(4), PiB(5), OOB(-1);

    fun next() = when (this) {
        B -> KiB
        KiB -> MiB
        MiB -> GiB
        GiB -> TiB
        TiB -> PiB
        PiB -> OOB
        OOB -> OOB
    }

}