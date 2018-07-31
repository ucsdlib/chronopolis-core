package org.chronopolis.rest.kot.entities

import javax.persistence.Entity
import javax.persistence.OneToMany

/**
 * Representation of a Node in Chronopolis
 *
 * todo: lateinit Sets?
 * todo: remove password/enabled... maybe rename username
 *
 * @author shake
 */
@Entity
class Node(
        @OneToMany(mappedBy = "node")
        var replications: Set<Replication> = emptySet(),
        var username: String = "",
        var password: String = "",
        var enabled: Boolean = true
) : PersistableEntity() {
        override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Node

                if (username != other.username) return false

                return true
        }

        override fun hashCode(): Int {
                return username.hashCode()
        }
}