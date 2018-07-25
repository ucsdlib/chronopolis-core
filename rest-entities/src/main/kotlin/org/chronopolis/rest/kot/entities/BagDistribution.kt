package org.chronopolis.rest.kot.entities

import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.ManyToOne

@Entity
class BagDistribution(
        @get:ManyToOne(fetch = FetchType.LAZY)
        var bag: Bag = Bag(),

        @get:ManyToOne(fetch = FetchType.LAZY)
        var node: Node = Node(),

        @get:Enumerated(value = EnumType.STRING)
        var status: BagDistributionStatus = BagDistributionStatus.DISTRIBUTE
) : PersistableEntity()