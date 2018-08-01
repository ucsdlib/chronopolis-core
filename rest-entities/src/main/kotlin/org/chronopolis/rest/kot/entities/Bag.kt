package org.chronopolis.rest.kot.entities

import com.google.common.collect.ComparisonChain
import org.chronopolis.rest.kot.entities.depositor.Depositor
import org.chronopolis.rest.kot.entities.storage.StagingStorage
import org.chronopolis.rest.kot.models.enums.BagStatus
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.PreUpdate

@Entity
class Bag(
        var name: String = "",

        var creator: String = "",

        @ManyToOne
        @JoinColumn(name = "depositor_id")
        var depositor: Depositor = Depositor(),

        var size: Long = 0L,

        var totalFiles: Long = 0L,

        @Enumerated(EnumType.STRING)
        var status: BagStatus = BagStatus.DEPOSITED
) : UpdatableEntity(), Comparable<Bag> {

    @OneToMany(mappedBy = "bag", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    lateinit var distributions: MutableSet<BagDistribution>

    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.EAGER)
    @JoinTable(name = "bag_storage",
            joinColumns = [(JoinColumn(name = "bag_id", referencedColumnName = "id"))],
            inverseJoinColumns = [JoinColumn(name = "staging_id", referencedColumnName = "id")])
    lateinit var bagStorage: MutableSet<StagingStorage>

    @ManyToMany(cascade = [CascadeType.PERSIST, CascadeType.MERGE], fetch = FetchType.EAGER)
    @JoinTable(name = "token_storage",
            joinColumns = [(JoinColumn(name = "bag_id", referencedColumnName = "id"))],
            inverseJoinColumns = [JoinColumn(name = "staging_id", referencedColumnName = "id")])
    lateinit var tokenStorage: MutableSet<StagingStorage>

    // Functions

    fun getReplicatingNodes(): Set<String> {
        return distributions
                .map { it.node.username }
                .toSet()
    }

    fun addDistribution(distribution: BagDistribution) {
        distributions.add(distribution)
    }

    fun addDistribution(node: Node, status: BagDistributionStatus) {
        distributions.add(BagDistribution(this, node, status))
    }

    override fun compareTo(other: Bag): Int {
        return ComparisonChain.start()
                .compare(depositor, other.depositor)
                .compare(name, other.name)
                .result()
    }

    @PreUpdate
    fun onUpdateBag() {
        val replicated = distributions.stream()
                .allMatch { it.status == BagDistributionStatus.REPLICATE }

        if (replicated && !distributions.isEmpty()) {
            status = BagStatus.PRESERVED
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bag

        if (name != other.name) return false
        if (creator != other.creator) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + creator.hashCode()
        return result
    }

}