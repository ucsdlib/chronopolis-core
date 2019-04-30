package org.chronopolis.db.result

import org.chronopolis.db.generated.tables.records.BagRecord

data class BagCreateResult(val bag: BagRecord? = null, val status: CreateStatus)