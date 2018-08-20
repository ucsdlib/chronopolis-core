package org.chronopolis.rest.entities

import javax.persistence.DiscriminatorValue
import javax.persistence.Entity

@Entity
@DiscriminatorValue("BAG")
class BagFile : DataFile()