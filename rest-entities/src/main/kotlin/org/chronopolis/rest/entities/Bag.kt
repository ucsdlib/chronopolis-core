package org.chronopolis.rest.entities

import com.google.common.collect.ComparisonChain
import org.chronopolis.rest.entities.depositor.Depositor
import org.chronopolis.rest.entities.storage.StagingStorage
import org.chronopolis.rest.models.enums.BagStatus
import org.hibernate.annotations.NaturalId
import javax.persistence.CascadeType.MERGE
import javax.persistence.CascadeType.PERSIST
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType.EAGER
import javax.persistence.FetchType.LAZY
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.PreUpdate

/**
 * A BagIt [Bag] stored by Chronopolis
 *
 * todo: creator should map to the user table (which means it will no longer be controlled by
 * spring-security)
 *
 * @property name a unique identifier for this [Bag]
 * @property creator the user which created the [Bag]
 * @property depositor the [Depositor] who owns the content of the [Bag]
 * @property size the size of the [Bag] in bytes
 * @property totalFiles the number of files contained within the [Bag]
 * @property status the status of the [Bag] in the Chronopolis Network
 * @property files a set of [BagFile]s which belong to the [Bag]
 * @property distributions a set of [BagDistribution]s defining the state of the [Bag] at distribution [Node]s
 *
 * @author shake
 */
@Entity
class Bag(
        @NaturalId
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

    @OneToMany(mappedBy = "bag", cascade = [MERGE, PERSIST], fetch = LAZY, orphanRemoval = true)
    var files: MutableSet<DataFile> = mutableSetOf()

    @OneToMany(mappedBy = "bag", cascade = [MERGE, PERSIST], fetch = LAZY, orphanRemoval = true)
    var storage: MutableSet<StagingStorage> = mutableSetOf()

    @OneToMany(mappedBy = "bag", cascade = [MERGE, PERSIST], fetch = EAGER, orphanRemoval = true)
    var distributions: MutableSet<BagDistribution> = mutableSetOf()

    // Functions

    fun getReplicatingNodes(): Set<String> {
        return distributions
                .map { it.node.username }
                .toSet()
    }

    fun addFile(file: DataFile) {
        files.add(file)
    }

    fun addFiles(toAdd: Set<DataFile>) {
        files.addAll(toAdd)
    }

    fun addStagingStorage(staging: StagingStorage) {
        storage.add(staging)
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