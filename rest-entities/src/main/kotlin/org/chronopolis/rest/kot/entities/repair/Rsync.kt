package org.chronopolis.rest.kot.entities.repair

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue("RSYNC")
class Rsync(var link: String) : Strategy()