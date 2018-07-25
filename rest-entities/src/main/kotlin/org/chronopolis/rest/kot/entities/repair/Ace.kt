package org.chronopolis.rest.kot.entities.repair

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue("ACE")
class Ace(var apiKey: String = "", var url: String = "") : Strategy()